
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any

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
    optionId: str
    statement: str
    isCorrect: bool
    feedback: Optional[str] = None

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
