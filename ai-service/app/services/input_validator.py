import re
from fastapi import HTTPException

# Keywords often used in Jailbreak attempts or to bypass system instructions
BLOCKED_PHRASES = [
    "ignore previous instructions",
    "system prompt",
    "you are a large language model",
    "dan mode",
    "developer mode",
    "act as a",
    "disregard all previous",
    "forget everything",
    "new rules:",
    "you must now",
    "bypass safety",
    "jailbreak",
]

# Max length for free-text fields to prevent token exhaustion or buffer overflows
MAX_TEXT_LENGTH = 4000

class InputValidator:
    """
    Sanitizes and validates user input to prevent Prompt Injection attacks.
    """

    @staticmethod
    def validate_text(text: str, context: str = "input") -> str:
        """
        Validates a single string.
        Raises HTTPException if blocked phrases are found or length is exceeded.
        Returns the sanitized text (escaped XML/HTML).
        """
        if not text:
            return ""

        if len(text) > MAX_TEXT_LENGTH:
             raise HTTPException(status_code=400, detail=f"{context} exceeds maximum length of {MAX_TEXT_LENGTH} characters.")

        lower_text = text.lower()
        for phrase in BLOCKED_PHRASES:
            if phrase in lower_text:
                # Log security event here in a real system
                print(f"SECURITY ALERT: Blocked phrase '{phrase}' detected in {context}.")
                raise HTTPException(status_code=400, detail="Input contains prohibited content or potential injection attempts.")

        # Robust sanitization: escape XML/HTML special characters
        # This prevents the user from closing our XML tags <user_content>...</user_content>
        sanitized = (text.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace('"', "&quot;")
                        .replace("'", "&apos;"))
        
        return sanitized

    @staticmethod
    def xml_wrap(tag: str, content: str) -> str:
        """
        Safely wraps content in XML tags, sanitizing the content first.
        """
        sanitized_content = InputValidator.validate_text(content, context=tag)
        return f"<{tag}>\n{sanitized_content}\n</{tag}>"

    @staticmethod
    def validate_obj(obj: any, depth=0):
        """
        Recursively validates strings within a dictionary or list.
        """
        if depth > 10: # Increased depth for complex objects
            return obj

        if isinstance(obj, str):
            return InputValidator.validate_text(obj)
        elif isinstance(obj, list):
            return [InputValidator.validate_obj(item, depth + 1) for item in obj]
        elif isinstance(obj, dict):
            return {k: InputValidator.validate_obj(v, depth + 1) for k, v in obj.items()}
        else:
            return obj

    @staticmethod
    def validate_uuid(text: str, context: str = "uuid") -> str:
        """
        Validates that the string is a valid UUID.
        """
        if not text:
             return text
        
        # Simple regex for UUID
        uuid_regex = re.compile(r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', re.I)
        if not uuid_regex.match(text):
             raise HTTPException(status_code=400, detail=f"Invalid UUID format in {context}: {text}")
        return text
