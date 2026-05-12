from fastapi import FastAPI, HTTPException
from contextlib import asynccontextmanager
import py_eureka_client.eureka_client as eureka_client
import os
import asyncio
import socket

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Retrieve env vars for Eureka
    eureka_server = os.getenv("EUREKA_URL", "http://eureka:8761/eureka/")
    instance_port = int(os.getenv("PORT", 8000))
    app_name = "ai-service"
    # Prefer explicit hostname (Railway) or fall back to container IP so other
    # services can always resolve us — container hostnames change on every rebuild.
    instance_host = os.getenv("EUREKA_INSTANCE_HOSTNAME") or socket.gethostbyname(socket.gethostname())

    print(f"Stats Eureka Client: {eureka_server} for {app_name}:{instance_port} host={instance_host}")
    init_kwargs = {
        "eureka_server": eureka_server,
        "app_name": app_name,
        "instance_port": instance_port,
        "instance_host": instance_host,
    }
    await eureka_client.init_async(**init_kwargs)
    yield
    # Stop Eureka Client
    print("Stopping Eureka Client")
    await eureka_client.stop_async()

from app.core import models
from app.services.llm_service import llm_service
from app.services.input_validator import InputValidator

app = FastAPI(title="AI Service", lifespan=lifespan)

# --- Endpoints ---

@app.get("/health")
def health():
    return {"status": "ok", "provider": llm_service.model if llm_service.client else "mock"}

@app.post("/v1/plans", response_model=models.GeneratePlanResponse)
def generate_plan(request: models.GeneratePlanRequest):
    try:
        val_profile = InputValidator.validate_obj(request.profile)
        val_goals = InputValidator.validate_obj(request.goals)
        val_catalog = InputValidator.validate_obj(request.contentCatalog)

        for goal in val_goals:
            if "domainId" in goal:
                InputValidator.validate_uuid(goal["domainId"], "goal.domainId")

        result = llm_service.generate_plan(
            user_profile=val_profile,
            goals=val_goals,
            content_catalog=val_catalog
        )
        return models.GeneratePlanResponse(
            plan=result.get("plan") or {},
            rawModelOutput=result.get("rawModelOutput") or {}
        )
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/plans/adjustments", response_model=models.ReplanResponse)
def replan(request: models.ReplanRequest):
    try:
        val_plan = InputValidator.validate_obj(request.currentPlan)
        val_events = InputValidator.validate_obj(request.recentEvents)
        val_skill = InputValidator.validate_obj(request.updatedSkillState)

        result = llm_service.replan(
            current_plan=val_plan,
            recent_events=val_events,
            skill_state=val_skill,
            reason=request.reason or ""
        )
        return models.ReplanResponse(
            plan=result.get("plan") or request.currentPlan or {},
            changeSummary=result.get("changeSummary") or "Adjustments applied based on performance."
        )
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/assessments/items", response_model=models.NextItemResponse)
def next_item(request: models.NextItemRequest):
    try:
        mastery = 0.5
        if request.skillState:
             mastery = sum([s.get("mastery", 0.5) for s in request.skillState]) / len(request.skillState)

        val_domain = InputValidator.validate_text(request.domain)
        val_history = InputValidator.validate_obj(request.recentHistory)
        val_context = None
        if request.contextText:
            val_context = InputValidator.validate_text(request.contextText, "contextText")

        result = llm_service.generate_next_item(
            domain=val_domain,
            mastery=mastery,
            recent_history=val_history,
            exclude_item_ids=request.excludeItemIds,
            context_text=val_context
        )
        return models.NextItemResponse(
            item=result.get("item") or {},
            rationale=result.get("rationale") or "Based on proficiency"
        )
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/assessments/feedback", response_model=models.FeedbackResponse)
def feedback(request: models.FeedbackRequest):
    try:
        item = request.item
        response = request.userResponse
        
        stem = item.get("stem", "Question")
        options = item.get("options", [])
        correct_opt = next((o for o in options if o.get("isCorrect")), None)
        correct_stmt = correct_opt.get("statement") if correct_opt else "Unknown"
        
        user_sel_id = response.get("selectedOptionId")
        user_opt = next((o for o in options if o.get("optionId") == user_sel_id), None)
        user_stmt = user_opt.get("statement") if user_opt else str(response.get("openAnswer", ""))
        
        is_correct = (user_sel_id == correct_opt.get("optionId")) if correct_opt and user_sel_id else False

        val_stem = InputValidator.validate_text(stem, "item_stem")
        val_correct = InputValidator.validate_text(correct_stmt, "correct_answer")
        val_user = InputValidator.validate_text(user_stmt, "user_answer")

        result = llm_service.generate_feedback(
            item_stem=val_stem,
            correct_answer=val_correct,
            user_answer=val_user,
            is_correct=is_correct
        )
        
        return models.FeedbackResponse(
            isCorrect=result.get("isCorrect") if result.get("isCorrect") is not None else is_correct,
            feedbackMessage=result.get("feedbackMessage") or "No feedback provided",
            remediationSuggestions=result.get("remediationSuggestions") or []
        )
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/assessments/diagnostic-tests", response_model=models.GenerateDiagnosticTestResponse)
def generate_diagnostic_test(request: models.GenerateDiagnosticTestRequest):
    try:
        InputValidator.validate_uuid(request.domainId)
        InputValidator.validate_text(request.level)

        result = llm_service.generate_diagnostic_test(
            domain=request.domainId,
            domain_name=request.domainName or request.domainId,
            level=request.level,
            n_questions=request.nQuestions
        )
        return models.GenerateDiagnosticTestResponse(questions=result.get("questions", []))
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/v1/contents/lessons", response_model=models.GenerateContentResponse)
def generate_lessons(request: models.GenerateContentRequest):
    try:
        val_domain = InputValidator.validate_text(request.domainId)
        val_level = InputValidator.validate_text(request.level)
        
        result = llm_service.generate_lessons(
            domain=val_domain,
            n_lessons=request.nLessons,
            level=val_level,
            difficulty=request.difficulty,
            locale=request.locale
        )
            
        return models.GenerateContentResponse(
            lessons=result.get("lessons") or [],
            assessmentItems=result.get("assessmentItems") or []
        )

    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/contents/assessment-items", response_model=models.GenerateAssessmentItemsResponse)
def generate_assessment_items(request: models.GenerateAssessmentItemsRequest):
    try:
        val_domain = InputValidator.validate_text(request.domainId, "domainId")
        val_context = None
        if request.contextText:
             val_context = InputValidator.validate_text(request.contextText, "contextText")
        
        result = llm_service.generate_assessment_items(
            context_text=val_context,
            n_items=request.nItems,
            domain=val_domain
        )
            
        return models.GenerateAssessmentItemsResponse(items=result.get("items", []))

    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/contents/skills", response_model=models.GenerateSkillsResponse)
def generate_skills(request: models.GenerateSkillsRequest):
    """US-10-06: Generate a taxonomy of skills for a given topic."""
    try:
        val_topic = InputValidator.validate_text(request.topic, "topic")
        val_domain = InputValidator.validate_text(request.domainId, "domainId")
        
        result = llm_service.generate_skills(
            topic=val_topic,
            domain_id=val_domain
        )
        
        return models.GenerateSkillsResponse(skills=result.get("skills", []))
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/contents/skills/prerequisites", response_model=models.GeneratePrerequisitesResponse)
def generate_prerequisites(request: models.GeneratePrerequisitesRequest):
    """US-10-07: Determine prerequisite relationships between skills."""
    try:
        val_skills = InputValidator.validate_obj(request.skills)
            
        result = llm_service.generate_prerequisites(skills=val_skills)
        
        return models.GeneratePrerequisitesResponse(prerequisites=result.get("prerequisites", []))
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/contents/skill-tags", response_model=models.AnalyzeSkillTagsResponse)
def analyze_skill_tags(request: models.AnalyzeSkillTagsRequest):
    """US-10-09: Analyze content and suggest relevant skill tags."""
    try:
        val_content = InputValidator.validate_text(request.contentText, "contentText")
        val_domain = InputValidator.validate_text(request.domainId, "domainId")
        
        result = llm_service.analyze_skill_tags(
            content_text=val_content,
            domain=val_domain
        )
        
        return models.AnalyzeSkillTagsResponse(suggestedSkillCodes=result.get("skill_codes", []))
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)



