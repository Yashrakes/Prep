package java_demo.java_demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculatorServiceTest {
    private CalculatorService service;

    @BeforeEach
    void setup() {
        service = new CalculatorService();
    }

    @Test
    void testAdd() {
        int result = service.add(2, 3);
        assertEquals(5, result);
    }
}