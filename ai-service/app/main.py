from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
import uuid
from app.services.llm_service import llm_service

app = FastAPI(title="AI Service")

# --- Models ---
# Using generic Dict for complex nested objects to stay flexible with OAS changes
# in a real implementation, we would duplicate Pydantic models from OAS.

class GeneratePlanRequest(BaseModel):
    userId: str
    profile: Dict[str, Any]
    goals: List[Dict[str, Any]]
    studyPreferences: Optional[Dict[str, Any]] = None
    contentCatalog: List[Dict[str, Any]] = []

class GeneratePlanResponse(BaseModel):
    plan: Dict[str, Any]
    rawModelOutput: Dict[str, Any] = {}

class ReplanRequest(BaseModel):
    userId: str
    currentPlan: Dict[str, Any]
    recentEvents: List[Dict[str, Any]] = []
    updatedSkillState: List[Dict[str, Any]] = []

class ReplanResponse(BaseModel):
    plan: Dict[str, Any]
    changeSummary: str

class NextItemRequest(BaseModel):
    userId: str
    domain: str
    skillState: List[Dict[str, Any]] = []
    recentHistory: List[Dict[str, Any]] = []

class NextItemResponse(BaseModel):
    item: Dict[str, Any]
    rationale: str

class FeedbackRequest(BaseModel):
    userId: str
    item: Dict[str, Any]
    userResponse: Dict[str, Any]
    skillStateBefore: List[Dict[str, Any]] = []

class FeedbackResponse(BaseModel):
    isCorrect: bool
    feedbackMessage: str
    remediationSuggestions: List[str] = []

# --- Endpoints ---

@app.get("/health")
def health():
    return {"status": "ok", "provider": llm_service.model if llm_service.client else "mock"}

@app.post("/v1/plans/generate", response_model=GeneratePlanResponse)
def generate_plan(request: GeneratePlanRequest):
    try:
        result = llm_service.generate_plan(
            user_profile=request.profile,
            goals=request.goals,
            content_catalog=request.contentCatalog
        )
        # The LLM should return keys "plan" and "rawModelOutput" or similar structure
        # We ensure strict validation in a production env, here we pass through for flexibility
        return GeneratePlanResponse(
            plan=result.get("plan", {}),
            rawModelOutput=result.get("rawModelOutput", {})
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/plans/replan", response_model=ReplanResponse)
def replan(request: ReplanRequest):
    try:
        result = llm_service.replan(
            current_plan=request.currentPlan,
            recent_events=request.recentEvents,
            skill_state=request.updatedSkillState
        )
        return ReplanResponse(
            plan=result.get("plan", request.currentPlan),
            changeSummary=result.get("changeSummary", "No changes applied.")
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/assessments/next-item", response_model=NextItemResponse)
def next_item(request: NextItemRequest):
    try:
        # Heuristic: Calculate average mastery from skillState for prompt context
        mastery = 0.5
        if request.skillState:
             mastery = sum([s.get("mastery", 0.5) for s in request.skillState]) / len(request.skillState)

        result = llm_service.generate_next_item(
            domain=request.domain,
            mastery=mastery,
            recent_history=request.recentHistory
        )
        return NextItemResponse(
            item=result.get("item", {}),
            rationale=result.get("rationale", "")
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/v1/assessments/feedback", response_model=FeedbackResponse)
def feedback(request: FeedbackRequest):
    try:
        item = request.item
        response = request.userResponse
        
        # Extract basic info safely
        stem = item.get("stem", "Question")
        # Assuming multiple choice for simplicity in finding correct answer
        options = item.get("options", [])
        correct_opt = next((o for o in options if o.get("isCorrect")), None)
        correct_stmt = correct_opt.get("statement") if correct_opt else "Unknown"
        
        user_sel_id = response.get("selectedOptionId")
        user_opt = next((o for o in options if o.get("optionId") == user_sel_id), None)
        user_stmt = user_opt.get("statement") if user_opt else str(response.get("openAnswer", ""))
        
        # Determine correctness (if not already simulated by frontend, AI validates it)
        # In a real system, the backend validates correctness deterministically for Quiz.
        # But for 'Open Answer' AI must judge. Here we assume AI judges.
        
        # If the request already contains correctness info (e.g. from Assessment Service), use it.
        # But the prompt expects us to judge or explain.
        
        # Let's assume we want AI to provide the explanation.
        is_correct = (user_sel_id == correct_opt.get("optionId")) if correct_opt and user_sel_id else False

        result = llm_service.generate_feedback(
            item_stem=stem,
            correct_answer=correct_stmt,
            user_answer=user_stmt,
            is_correct=is_correct
        )
        
        return FeedbackResponse(
            isCorrect=result.get("isCorrect", is_correct),
            feedbackMessage=result.get("feedbackMessage", ""),
            remediationSuggestions=result.get("remediationSuggestions", [])
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

class GenerateContentRequest(BaseModel):
    domain: str # Code or Name
    skillIds: Optional[List[str]] = None
    nLessons: int = 3
    level: str = "beginner"
    difficulty: Optional[float] = 0.5
    locale: str = "es-ES"

class GenerateContentResponse(BaseModel):
    lessons: List[Dict[str, Any]]

@app.post("/v1/content/generate-lessons", response_model=GenerateContentResponse)
def generate_lessons(request: GenerateContentRequest):
    try:
        # Map request to service
        result = llm_service.generate_lessons(
            domain=request.domain,
            n_lessons=request.nLessons,
            locale=request.locale
        )
        return GenerateContentResponse(lessons=result.get("lessons", []))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)


