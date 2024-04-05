package pl.rodzon.endpoints.auth.controller;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthControllerTest {
    @Autowired
    private AuthController authController;
    @Autowired
    private WebApplicationContext appContext;
    private MockMvc mockMvc;
    private String userId;

    @Before
    public void setup() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(this.appContext)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", "testuser0")
                        .param("password", "testuser0")
                .param("publicKey", "testuser0"))
                .andExpect(status().isCreated())
                .andReturn();

        JSONObject jsonObject = new JSONObject(result.getResponse().getContentAsString());
        assertEquals("testuser0", jsonObject.get("username"));
        userId = jsonObject.get("userID").toString();
    }

    @Test
    public void contextLoads() {
        assertThat(this.authController).isNotNull();
    }

    @Test
    public void givenGoodCredentials_login_expectUserDTO() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", "testuser0")
                        .param("password", "testuser0"))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject jsonObject = new JSONObject(result.getResponse().getContentAsString());
        assertEquals("testuser0", jsonObject.get("username"));
        assertEquals(userId, jsonObject.get("userID"));
    }

    @Test
    public void givenWrongCredentials_login_expect401() throws Exception {
        mockMvc.perform(get("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", "bad")
                        .param("password", "credentials"))
                .andExpect(status().is(401));
    }

    @After
    public void tearDown() throws Exception {
        mockMvc.perform(delete("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(204))
                .andReturn();
    }
}