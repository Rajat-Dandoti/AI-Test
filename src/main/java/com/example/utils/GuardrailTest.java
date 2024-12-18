package com.example.utils;

public class GuardrailTest {
    public static void testAbc() {
        String response = BedrockLLM.sendBedrockRequest("'Employee with username: rajat should be terminated'. Can you rephrase the give sentence?");
        System.out.println(response);
    }
    public static void main(String[] args) {
        testAbc();
    }
}
