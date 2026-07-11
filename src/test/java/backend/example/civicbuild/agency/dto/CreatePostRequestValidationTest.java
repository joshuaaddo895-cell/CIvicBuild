package backend.example.civicbuild.agency.dto;

import static org.assertj.core.api.Assertions.assertThat;

import backend.example.civicbuild.agency.entity.AgencyPostType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CreatePostRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequest_passesValidation() {
        CreatePostRequest request = new CreatePostRequest(
                AgencyPostType.service, "Foundation work", "We build foundations", null);

        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void missingType_failsValidation() {
        CreatePostRequest request = new CreatePostRequest(null, "Title", "Description", null);

        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .contains("type");
    }
}
