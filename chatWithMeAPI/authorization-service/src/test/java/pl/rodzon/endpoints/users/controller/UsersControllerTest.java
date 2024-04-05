package pl.rodzon.endpoints.users.controller;

import org.json.JSONArray;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UsersControllerTest {
    @Autowired
    private UsersController usersController;
    @Autowired
    private WebApplicationContext appContext;
    private MockMvc mockMvc;
    private List<String> usersIds = new ArrayList<>();

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
        usersIds.add(jsonObject.get("userID").toString());

        MvcResult result2 = mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", "testuser1")
                        .param("password", "testuser1")
                        .param("publicKey", "testuser1"))
                .andExpect(status().isCreated())
                .andReturn();

        JSONObject jsonObject2 = new JSONObject(result2.getResponse().getContentAsString());
        assertEquals("testuser1", jsonObject2.get("username"));
        usersIds.add(jsonObject2.get("userID").toString());
    }

    @Test
    public void contextLoads() {
        assertThat(this.usersController).isNotNull();
    }

    @Test
    public void givenNothing_getAllUsersNotPageable_expectUsersDTO() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("isPageableEnabled", "false"))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject jsonObject = new JSONObject(result.getResponse().getContentAsString());
        JSONArray jsonArray = jsonObject.getJSONArray("users");
        JSONObject userOne = (JSONObject) jsonArray.get(0);
        assertEquals("testuser0", userOne.getString("username"));
        JSONObject userTwo = (JSONObject) jsonArray.get(1);
        assertEquals("testuser1", userTwo.getString("username"));
    }

    @After
    public void tearDown() throws Exception {
        mockMvc.perform(delete("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("usersIDs", usersIds.get(0))
                        .param("usersIDs", usersIds.get(1)))
                .andExpect(status().is(204))
                .andReturn();
    }
}