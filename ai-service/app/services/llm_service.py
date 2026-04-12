
import json
import logging
import uuid
from typing import Dict, Any, List, Optional
from pydantic import ValidationError
from openai import OpenAI, OpenAIError
from app.core.config import settings
from app.core import prompts
from app.services.input_validator import InputValidator
from app.core.models import PlanStructure

logger = logging.getLogger(__name__)

class LLMService:
    def __init__(self):
        self.api_key = settings.OPENAI_API_KEY
        self.model = settings.OPENAI_MODEL
        self.client = None
        
        if settings.USE_MOCK_AI:
             logger.info("USE_MOCK_AI=true. Running in MOCK mode.")
             self.client = None
        elif self.api_key:
            self.client = OpenAI(api_key=self.api_key)
        elif settings.ENVIRONMENT == "test":
            logger.info("Test Environment detected. Running in MOCK mode.")
            self.client = None
        else:
            logger.warning("Unexpected state. Defaulting to MOCK mode.")
            self.client = None

    def _call_llm(self, system_prompt: str, user_prompt: str, response_format: str = "json_object") -> Dict[str, Any]:
        """Generic method to call OpenAI Chat Completion with structured output."""
        if not self.client:
            raise ValueError("OpenAI Client not initialized. Check API Key.")

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                response_format={"type": response_format},
                temperature=0.7,
            )
            content = response.choices[0].message.content
            return json.loads(content)
        except OpenAIError as e:
            logger.error(f"OpenAI API Error: {e}")
            raise e
        except json.JSONDecodeError:
            logger.error(f"Failed to decode JSON from LLM: {content}")
            raise ValueError("Invalid JSON response from LLM")
        except Exception as e:
            logger.error(f"Generic Error in call_llm: {e}")
            raise e

    def _audit_content(self, original_prompt: str, response_to_audit: Dict[str, Any]) -> Dict[str, Any]:
        """US-10-12: Audit generated content for safety, accuracy and structure."""
        if not self.client:
            return response_to_audit

        audit_user_prompt = f"Original Goal: {original_prompt}\nResponse to audit: {json.dumps(response_to_audit)}"
        try:
            audit_result = self._call_llm(prompts.CONTENT_AUDIT_PROMPT, audit_user_prompt)
            if not audit_result.get("isValid", True):
                logger.warning(f"Content Audit Failed: {audit_result.get('issues')}")
                if audit_result.get("correctedResponse"):
                    logger.info("Applying corrected response from Audit layer.")
                    return audit_result["correctedResponse"]
            return response_to_audit
        except Exception as e:
            logger.error(f"Audit Layer Error: {e}")
            return response_to_audit # Fallback to original response on audit failure

    def _ensure_plan_contract(self, data: Dict[str, Any]) -> Dict[str, Any]:
        """US-10-14: Ensures LLM response complies with PlanStructure schema."""
        plan_data = data.get("plan", data)
        
        if not isinstance(plan_data, dict):
            plan_data = {"modules": []}
            
        if "modules" not in plan_data or not isinstance(plan_data["modules"], list):
            plan_data["modules"] = []
            
        for module in plan_data["modules"]:
            if not isinstance(module, dict): continue
            module.setdefault("title", "Module")
            module.setdefault("description", "Learning material")
            if "activities" not in module or not isinstance(module["activities"], list):
                module["activities"] = []
            for activity in module["activities"]:
                if not isinstance(activity, dict): continue
                if "type" not in activity and "activityType" in activity:
                    activity["type"] = activity["activityType"]
                activity.setdefault("title", "Activity")
                activity.setdefault("type", "lesson")
                if not activity.get("contentRef"):
                    activity["contentRef"] = f"manual:{uuid.uuid4()}"
        
        try:
            return PlanStructure(**plan_data).model_dump()
        except ValidationError as e:
            logger.warning(f"Plan validation failed: {e}. Using best-effort.")
            return PlanStructure.construct(**plan_data).model_dump()

    def generate_plan(self, user_profile: Dict, goals: List[Dict], content_catalog: List[Dict]) -> Dict[str, Any]:
        if not self.client:
            return self._mock_plan(user_profile)

        # US-10-10: Wrap context in XML tags to prevent prompt injection
        user_context_xml = (
            InputValidator.xml_wrap("user_profile", json.dumps(user_profile)) + "\n" +
            InputValidator.xml_wrap("goals", json.dumps(goals)) + "\n" +
            InputValidator.xml_wrap("content_catalog", json.dumps(content_catalog))
        )
        
        full_user_prompt = f"<user_context>\n{user_context_xml}\n</user_context>"
        raw_response = self._call_llm(prompts.PLAN_GENERATION_SYSTEM_PROMPT, full_user_prompt)
        
        # Robust validation and repair
        validated_plan = self._ensure_plan_contract(raw_response)
        
        # Apply Audit
        final_plan = self._audit_content("Generate personalized learning plan", validated_plan)
        
        return {"plan": final_plan, "rawModelOutput": raw_response}

    def replan(self, current_plan: Dict, recent_events: List[Dict], skill_state: List[Dict], reason: str = "") -> Dict[str, Any]:
        if not self.client:
            return self._mock_replan(current_plan)

        user_context_xml = (
            InputValidator.xml_wrap("current_plan", json.dumps(current_plan)) + "\n" +
            InputValidator.xml_wrap("recent_events", json.dumps(recent_events)) + "\n" +
            InputValidator.xml_wrap("skill_state", json.dumps(skill_state))
        )
        
        full_user_prompt = f"<user_context>\n{user_context_xml}\n</user_context>"
        formatted_system = prompts.REPLAN_SYSTEM_PROMPT.format(reason=reason or "Not specified")
        
        raw_response = self._call_llm(formatted_system, full_user_prompt)
        
        # Robust validation and repair
        validated_plan = self._ensure_plan_contract(raw_response)
        
        # Apply Audit
        final_with_audit = self._audit_content(f"Replan due to {reason}", validated_plan)
        
        # Replan response format expects 'plan' (structure) and 'changeSummary'
        return {
            "plan": final_with_audit, 
            "changeSummary": raw_response.get("changeSummary", "Adjustments applied based on performance.")
        }

    def generate_next_item(self, domain: str, mastery: float, recent_history: List[Dict], exclude_item_ids: List[str] = [], context_text: Optional[str] = None) -> Dict[str, Any]:
        if not self.client:
            return self._mock_item(domain)

        context_xml = (
            InputValidator.xml_wrap("domain", domain) + "\n" +
            InputValidator.xml_wrap("mastery", str(mastery)) + "\n" +
            InputValidator.xml_wrap("history", json.dumps(recent_history)) + "\n" +
            InputValidator.xml_wrap("exclude_ids", json.dumps(exclude_item_ids))
        )
        
        if context_text:
            context_xml += "\n" + InputValidator.xml_wrap("lesson_context", context_text)
        
        full_user_prompt = f"<context>\n{context_xml}\n</context>"
        system_prompt = prompts.NEXT_ITEM_SYSTEM_PROMPT.format(domain=domain, mastery=mastery)
        
        raw_response = self._call_llm(system_prompt, full_user_prompt)
        return self._audit_content(f"Generate next adaptive item for {domain}", raw_response)

    def generate_feedback(self, item_stem: str, correct_answer: str, user_answer: str, is_correct: bool) -> Dict[str, Any]:
        if not self.client:
            return {"isCorrect": is_correct, "feedbackMessage": "Mock Feedback: Good job."}

        # US-10-10: Sanitize student answer specifically as it's the highest risk
        safe_user_answer = InputValidator.validate_text(user_answer, context="user_answer")
        
        user_content = json.dumps({
            "stem": item_stem,
            "correct_answer": correct_answer,
            "user_answer": safe_user_answer,
            "is_correct": is_correct
        })
        
        formatted_system = prompts.FEEDBACK_SYSTEM_PROMPT.format(
            stem=item_stem, 
            correct_answer=correct_answer, 
            user_answer=safe_user_answer, 
            is_correct=is_correct
        )
        
        raw_response = self._call_llm(formatted_system, user_content)
        return self._audit_content("Generate feedback for student answer", raw_response)

    def generate_lessons(self, domain: str, n_lessons: int, level: str = "beginner", difficulty: float = 0.5, locale: str = "es-ES") -> Dict[str, Any]:
        if not self.client:
            return self._mock_lessons(domain, difficulty)
        
        system_prompt = prompts.CONTENT_GENERATION_SYSTEM_PROMPT.format(
            domain=domain, 
            n_lessons=n_lessons, 
            level=level,
            locale=locale
        )
        user_prompt = f"Generate lessons for topic: <topic>{domain}</topic>"
        
        raw_response = self._call_llm(system_prompt, user_prompt)
        return self._audit_content(f"Generate {n_lessons} lessons for {domain}", raw_response)

    def generate_diagnostic_test(self, domain: str, domain_name: str, level: str, n_questions: int) -> Dict[str, Any]:
        if not self.client:
            return {"questions": [{"stem": "Mock?", "options": [], "difficulty": 0.5, "topic": domain}]}

        system_prompt = prompts.DIAGNOSTIC_GENERATION_PROMPT.format(
            domain=domain,
            domain_name=domain_name or domain,
            level=level,
            n_questions=n_questions
        )
        user_prompt = f"Generate diagnostic test for: {domain_name or domain}"

        MAX_RETRIES = 2
        for attempt in range(1 + MAX_RETRIES):
            result = self._call_llm(system_prompt, user_prompt)
            questions = result.get("questions", [])
            if len(questions) == n_questions:
                return self._audit_content(f"Audit diagnostic for {domain_name}", result)
            
            # If length is slightly off, we still audit and potentially correct
            if attempt == MAX_RETRIES:
                return self._audit_content(f"Audit diagnostic for {domain_name}", result)

        return result

    # --- Mocks for Fallback ---
    def _mock_plan(self, profile):
        return {"plan": {"planId": "mock-id", "userId": profile.get("userId"), "modules": []}, "rawModelOutput": {}}

    def _mock_replan(self, current_plan):
        return {"plan": current_plan, "changeSummary": "Mock Replan: No changes applied."}

    def _mock_item(self, domain):
        return {"item": {"type": "multiple_choice", "stem": f"Mock question for {domain}", "options": [], "difficulty": 0.5}}

    def _mock_lessons(self, domain, difficulty):
        return {"lessons": [{"title": f"Mock Lesson for {domain}", "body": "...", "difficulty": difficulty}]}

    def generate_skills(self, topic: str, domain_id: str) -> Dict[str, Any]:
        if not self.client: return {"skills": []}
        system_prompt = prompts.SKILL_TAXONOMY_PROMPT.format(topic=topic, domain_id=domain_id)
        return self._call_llm(system_prompt, f"Skills for {topic}")

    def generate_prerequisites(self, skills: List[Dict[str, Any]]) -> Dict[str, Any]:
        if not self.client: return {"prerequisites": []}
        return self._call_llm(prompts.PREREQUISITE_GRAPH_PROMPT, json.dumps(skills))

    def generate_assessment_items(self, context_text: str, n_items: int, domain: str) -> Dict[str, Any]:
        if not self.client: return {"items": []}
        system_prompt = prompts.ASSESSMENT_GENERATION_PROMPT.format(context=context_text, domain=domain, n_items=n_items)
        return self._call_llm(system_prompt, f"Generate {n_items} items")

    def analyze_skill_tags(self, content_text: str, domain: str) -> Dict[str, Any]:
        if not self.client: return {"skill_codes": []}
        system_prompt = prompts.SKILL_TAGGING_PROMPT.format(content=content_text, domain=domain)
        return self._call_llm(system_prompt, "Extract skill codes")

llm_service = LLMService()
