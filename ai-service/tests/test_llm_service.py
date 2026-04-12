import pytest
from unittest.mock import Mock, patch, MagicMock
import json
import sys
import os

# Add app to path
sys.path.append(os.path.join(os.path.dirname(__file__), ".."))

# 1. Mock OpenAI module BEFORE importing app.services.llm_service
# We need to ensure 'OpenAI' and 'OpenAIError' are available
mock_openai_module = MagicMock()
# Define a real exception class for OpenAIError so pytest.raises works
class MockOpenAIError(Exception):
    pass
mock_openai_module.OpenAIError = MockOpenAIError
sys.modules['openai'] = mock_openai_module

from app.services.llm_service import LLMService

@pytest.fixture
def mock_openai_client():
    """Create a mock OpenAI client"""
    mock_client = Mock()
    mock_response = Mock()
    mock_choice = Mock()
    mock_message = Mock()
    
    mock_message.content = json.dumps({"test": "response"})
    mock_choice.message = mock_message
    mock_response.choices = [mock_choice]
    mock_client.chat.completions.create.return_value = mock_response
    
    return mock_client

@pytest.fixture
def llm_service_with_mock(mock_openai_client):
    """Create LLMService with mocked OpenAI client"""
    # We mock 'app.services.llm_service.OpenAI' to return our mock client class/instance
    with patch('app.services.llm_service.OpenAI') as mock_openai_class:
        mock_openai_class.return_value = mock_openai_client
        
        # Also need to ensure settings don't force mock mode during init for this fixture
        with patch('app.services.llm_service.settings') as mock_settings:
            mock_settings.OPENAI_API_KEY = "test-fixture-key"
            mock_settings.OPENAI_MODEL = "gpt-4"
            mock_settings.USE_MOCK_AI = False
            mock_settings.ENVIRONMENT = "production"
            
            service = LLMService()
            # Explicitly set client to be safe, though init should have done it
            service.client = mock_openai_client
            service.api_key = "test-fixture-key" 
            yield service

def test_llm_service_initialization_with_api_key():
    """Test LLMService initializes with API key"""
    # Patch settings at the source where they are imported in llm_service.py
    with patch('app.services.llm_service.settings') as mock_settings:
        mock_settings.OPENAI_API_KEY = "test-key"
        mock_settings.OPENAI_MODEL = "gpt-4"
        mock_settings.USE_MOCK_AI = False
        mock_settings.ENVIRONMENT = "production"
        
        with patch('app.services.llm_service.OpenAI') as mock_openai:
            service = LLMService()
            assert service.api_key == "test-key"
            assert service.client is not None # Assuming this calls OpenAI()

def test_llm_service_initialization_without_api_key():
    """Test LLMService initializes in mock mode without API key"""
    with patch('app.services.llm_service.settings') as mock_settings:
        mock_settings.OPENAI_API_KEY = None
        mock_settings.OPENAI_MODEL = "gpt-4"
        mock_settings.USE_MOCK_AI = False
        
        service = LLMService()
        assert service.client is None

def test_generate_plan_with_client(llm_service_with_mock, mock_openai_client):
    """Test plan generation with OpenAI client"""
    mock_openai_client.chat.completions.create.return_value.choices[0].message.content = json.dumps({
        "plan": {"planId": "test-123", "modules": []},
        "rawModelOutput": {}
    })
    
    result = llm_service_with_mock.generate_plan(
        user_profile={"userId": "user-123"},
        goals=[{"title": "Learn Python"}],
        content_catalog=[]
    )
    
    assert "plan" in result
    assert result["plan"]["planId"] == "test-123"
    assert mock_openai_client.chat.completions.create.call_count >= 1

def test_generate_plan_without_client():
    """Test plan generation falls back to mock when no client"""
    service = LLMService()
    service.client = None
    
    result = service.generate_plan(
        user_profile={"userId": "user-123"},
        goals=[],
        content_catalog=[]
    )
    
    assert "plan" in result
    assert result["plan"]["userId"] == "user-123"
    assert "rawModelOutput" in result

def test_replan_with_client(llm_service_with_mock, mock_openai_client):
    """Test replan with OpenAI client"""
    mock_openai_client.chat.completions.create.return_value.choices[0].message.content = json.dumps({
        "plan": {"updated": True},
        "changeSummary": "Updated modules"
    })
    
    result = llm_service_with_mock.replan(
        current_plan={"planId": "old-plan"},
        recent_events=[],
        skill_state=[]
    )
    
    assert "changeSummary" in result
    assert result["changeSummary"] == "Updated modules"

def test_replan_without_client():
    """Test replan falls back to mock"""
    service = LLMService()
    service.client = None
    
    current_plan = {"planId": "test-plan"}
    result = service.replan(
        current_plan=current_plan,
        recent_events=[],
        skill_state=[]
    )
    
    assert result["plan"] == current_plan
    assert "Mock Replan" in result["changeSummary"]

def test_generate_next_item_with_client(llm_service_with_mock, mock_openai_client):
    """Test next item generation with OpenAI client"""
    mock_openai_client.chat.completions.create.return_value.choices[0].message.content = json.dumps({
        "item": {"stem": "Test question", "type": "multiple_choice"},
        "rationale": "Testing knowledge"
    })
    
    result = llm_service_with_mock.generate_next_item(
        domain="Python",
        mastery=0.5,
        recent_history=[]
    )
    
    assert "item" in result
    assert result["item"]["stem"] == "Test question"

def test_generate_next_item_without_client():
    """Test next item generation falls back to mock"""
    service = LLMService()
    service.client = None
    
    result = service.generate_next_item(
        domain="Python",
        mastery=0.5,
        recent_history=[]
    )
    
    assert "item" in result
    assert "Mock question for Python" in result["item"]["stem"]

def test_generate_feedback_with_client(llm_service_with_mock, mock_openai_client):
    """Test feedback generation with OpenAI client"""
    mock_openai_client.chat.completions.create.return_value.choices[0].message.content = json.dumps({
        "isCorrect": True,
        "feedbackMessage": "Excellent work!",
        "remediationSuggestions": []
    })
    
    result = llm_service_with_mock.generate_feedback(
        item_stem="What is 2+2?",
        correct_answer="4",
        user_answer="4",
        is_correct=True
    )
    
    assert result["isCorrect"] == True
    assert "Excellent" in result["feedbackMessage"]

def test_generate_feedback_without_client():
    """Test feedback generation falls back to mock"""
    service = LLMService()
    service.client = None
    
    result = service.generate_feedback(
        item_stem="Test",
        correct_answer="A",
        user_answer="A",
        is_correct=True
    )
    
    assert result["isCorrect"] == True
    assert "Mock Feedback" in result["feedbackMessage"]

def test_generate_lessons_with_client(llm_service_with_mock, mock_openai_client):
    """Test lesson generation with OpenAI client"""
    mock_openai_client.chat.completions.create.return_value.choices[0].message.content = json.dumps({
        "lessons": [
            {"title": "Lesson 1", "body": "Content", "estimatedMinutes": 30}
        ]
    })
    
    result = llm_service_with_mock.generate_lessons(
        domain="Python",
        n_lessons=1,
        locale="es-ES"
    )
    
    assert "lessons" in result
    assert len(result["lessons"]) == 1

def test_generate_lessons_without_client():
    """Test lesson generation falls back to mock"""
    service = LLMService()
    service.client = None
    
    result = service.generate_lessons(
        domain="Python",
        n_lessons=3,
        locale="es-ES"
    )
    
    assert "lessons" in result
    assert "Mock Lesson for Python" in result["lessons"][0]["title"]

def test_call_llm_json_decode_error(llm_service_with_mock, mock_openai_client):
    """Test that JSON decode errors are handled"""
    mock_openai_client.chat.completions.create.return_value.choices[0].message.content = "Invalid JSON"
    
    with pytest.raises(ValueError, match="Invalid JSON response"):
        llm_service_with_mock._call_llm("system", "user")

def test_call_llm_openai_error(llm_service_with_mock, mock_openai_client):
    """Test that OpenAI errors are propagated"""
    # Use the mocked exception class we defined
    from openai import OpenAIError
    mock_openai_client.chat.completions.create.side_effect = OpenAIError("API Error")
    
    with pytest.raises(OpenAIError):
        llm_service_with_mock._call_llm("system", "user")

def test_call_llm_without_client():
    """Test that calling LLM without client raises error"""
    service = LLMService()
    service.client = None
    
    with pytest.raises(ValueError, match="OpenAI Client not initialized"):
        service._call_llm("system", "user")
