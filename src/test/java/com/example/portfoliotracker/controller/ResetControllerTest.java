package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.service.ResetService;
import com.example.portfoliotracker.service.ResetService.ResetSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResetController.class)
@SuppressWarnings("deprecation")
class ResetControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ResetService resetService;

    @Test
    void resetUserData_returnsSuccessSummary() throws Exception {
        ResetSummary summary = new ResetSummary();
        summary.setUsersDeleted(5);
        summary.setTransactionsDeleted(10);
        summary.setHoldingsDeleted(8);
        summary.setStatus("SUCCESS");
        summary.setMessage("All user-related data has been cleared");

        when(resetService.resetUserData()).thenReturn(summary);

        mvc.perform(delete("/api/reset/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.usersDeleted").value(5))
                .andExpect(jsonPath("$.transactionsDeleted").value(10))
                .andExpect(jsonPath("$.holdingsDeleted").value(8))
                .andExpect(jsonPath("$.message").value("All user-related data has been cleared"));
    }

    @Test
    void resetHealth_returnsWarningMessage() throws Exception {
        mvc.perform(delete("/api/reset/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(containsString("Reset controller is active")))
                .andExpect(jsonPath("$").value(containsString("development/testing only")))
                .andExpect(jsonPath("$").value(containsString("disabled in production")));
    }
}

