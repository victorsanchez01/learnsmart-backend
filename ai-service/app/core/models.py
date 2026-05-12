
from pydantic import BaseModel, Field, field_validator
from typing import List, Optional, Dict, Any

# Level→numeric difficulty mapping used to defend against LLM responses that
# return categorical levels (e.g. "BEGINNER") where a 0.0-1.0 difficulty is
# expected. Keep this map lower-cased; lookups normalise the input.
_LEVEL_TO_DIFFICULTY = {
    "beginner": 0.3,
    "intermediate": 0.5,
    "advanced": 0.8,
    "easy": 0.2,
    "medium": 0.5,
    "hard": 0.8,
}


def _coerce_difficulty(v):
    """Best-effort conversion of the LLM's difficulty field into a float in
    [0.0, 1.0]. Accepts already-numeric values, numeric strings, and the
    well-known level keywords. Falls back to 0.5 if unparseable."""
    if isinstance(v, (int, float)):
        return float(v)
    if isinstance(v, str):
        mapped = _LEVEL_TO_DIFFICULTY.get(v.strip().lower())
        if mapped is not None:
            return mapped
        try:
            return float(v)
        except ValueError:
            return 0.5
    return 0.5

# --- AI Workflow Models (Internal Contract) ---
class ActivityDraft(BaseModel):
    title: str = "Learning Activity"
    type: str = "lesson"
    contentRef: str = "manual:pending"

class ModuleDraft(BaseModel):
    title: str = "Module"
    description: str = "Educational content"
    position: Optional[int] = None
    activities: List[ActivityDraft] = Field(default_factory=list)

class PlanStructure(BaseModel):
    planId: Optional[str] = None
    userId: Optional[str] = None
    modules: List[ModuleDraft] = Field(default_factory=list)

# --- Audit Models ---
class ContentAuditResponse(BaseModel):
    isValid: bool
    issues: List[str] = []
    correctedResponse: Optional[Dict[str, Any]] = None

# --- Request/Response Models ---

class GeneratePlanRequest(BaseModel):
    userId: str
    profile: Dict[str, Any]
    goals: List[Dict[str, Any]]
    studyPreferences: Optional[Dict[str, Any]] = None
    contentCatalog: List[Dict[str, Any]] = []

class GeneratePlanResponse(BaseModel):
    plan: PlanStructure
    rawModelOutput: Dict[str, Any] = {}

class ReplanRequest(BaseModel):
    userId: str
    reason: Optional[str] = None
    currentPlan: Dict[str, Any]
    recentEvents: List[Dict[str, Any]] = []
    updatedSkillState: List[Dict[str, Any]] = []

class ReplanResponse(BaseModel):
    plan: PlanStructure
    changeSummary: str

class NextItemRequest(BaseModel):
    userId: str
    domain: str
    skillState: List[Dict[str, Any]] = []
    recentHistory: List[Dict[str, Any]] = []
    excludeItemIds: List[str] = []
    contextText: Optional[str] = None

class AssessmentOption(BaseModel):
    # The LLM is asked to produce pedagogical content only; identifiers are the
    # responsibility of the consuming service (assessment-service assigns UUIDs
    # at persist time). We therefore accept the LLM's natural output shape
    # (label + feedbackTemplate) and keep optionId/feedback optional so callers
    # that already provide UUIDs continue to work.
    optionId: Optional[str] = None
    label: Optional[str] = None
    statement: str = ""
    isCorrect: bool = False
    feedback: Optional[str] = None
    feedbackTemplate: Optional[str] = None

class AssessmentItem(BaseModel):
    type: str = "multiple_choice"
    stem: str = "Question"
    options: List[AssessmentOption] = []
    difficulty: float = 0.5

class NextItemResponse(BaseModel):
    item: AssessmentItem = Field(default_factory=AssessmentItem)
    rationale: str = "Based on student proficiency"

class FeedbackRequest(BaseModel):
    userId: str
    item: Dict[str, Any]
    userResponse: Dict[str, Any]
    skillStateBefore: List[Dict[str, Any]] = []

class FeedbackResponse(BaseModel):
    isCorrect: bool = False
    feedbackMessage: str = "Educational feedback"
    remediationSuggestions: List[str] = Field(default_factory=list)

class GenerateDiagnosticTestRequest(BaseModel):
    domainId: str
    domainName: Optional[str] = None
    level: str = "BEGINNER"
    nQuestions: int = 5

class DiagnosticQuestion(BaseModel):
    stem: str = "Question"
    options: List[Dict[str, Any]] = []
    difficulty: float = 0.5
    topic: str = "General"

class GenerateDiagnosticTestResponse(BaseModel):
    questions: List[DiagnosticQuestion] = []

class GenerateContentRequest(BaseModel):
    domainId: str
    skillIds: Optional[List[str]] = None
    nLessons: int = 3
    level: str = "beginner"
    difficulty: Optional[float] = 0.5
    locale: str = "es-ES"

class Lesson(BaseModel):
    title: str = "Lesson"
    description: str = ""
    body: str = ""
    estimatedMinutes: int = 10
    difficulty: float = 0.5
    type: str = "lesson"

    @field_validator("difficulty", mode="before")
    @classmethod
    def _coerce_difficulty(cls, v):
        return _coerce_difficulty(v)

class GenerateContentResponse(BaseModel):
    lessons: List[Lesson] = Field(default_factory=list)
    assessmentItems: Optional[List[Dict[str, Any]]] = None

class GenerateAssessmentItemsRequest(BaseModel):
    domainId: str
    skillIds: Optional[List[str]] = None
    nItems: int = 5
    itemType: str = "multiple_choice"
    difficultyRange: Optional[Dict[str, float]] = None
    locale: str = "es-ES"
    contextText: Optional[str] = None

class GenerateAssessmentItemsResponse(BaseModel):
    items: List[Dict[str, Any]]

class GenerateSkillsRequest(BaseModel):
    topic: str
    domainId: str

class SkillDefinition(BaseModel):
    code: str
    name: str = "Skill"
    description: str = ""
    level: str = "BEGINNER"
    tags: List[str] = []

class GenerateSkillsResponse(BaseModel):
    skills: List[SkillDefinition]

class GeneratePrerequisitesRequest(BaseModel):
    skills: List[Dict[str, Any]]

class GeneratePrerequisitesResponse(BaseModel):
    prerequisites: List[Dict[str, Any]]

class AnalyzeSkillTagsRequest(BaseModel):
    contentText: str
    domainId: str

class AnalyzeSkillTagsResponse(BaseModel):
    suggestedSkillCodes: List[str]
