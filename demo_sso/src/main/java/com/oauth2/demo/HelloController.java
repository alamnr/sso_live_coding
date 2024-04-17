package com.oauth2.demo;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class HelloController {
    
    private static final String ACCESS_TOKEN = "access_token";
    private static final Logger LOGGER = LoggerFactory.getLogger(HelloController.class);
    public static final String AUTHORIZATION_SUCCESS_PATH = "/oauth2/callback";

    static final String REDIRECT_URI = "http://localhost:8080" + AUTHORIZATION_SUCCESS_PATH;

    private static final String TOKEN_URI ="https://dev-47658814.okta.com/oauth2/default/v1/token";

    private static final String AUTHORIZATION_END_POINT = "https://dev-47658814.okta.com/oauth2/default/v1/authorize";
    private static final String ATTRIBUTES = "attributes";

    
    @Value("${SSO_CLIENT_ID}")
    private String clientId;

    @Value("${SSO_CLIENT_SECRET}")
    private String clientSecret;
    

    private final RestClient restClient;

    private  ObjectMapper objectMapper = new ObjectMapper();
    
    public HelloController(RestClient.Builder builder) {
        this.restClient = builder.build();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    }
    private static final String USERNAME_AATRIBUTE = "username";


    @GetMapping("/")
    public String index(HttpServletRequest request, Model model){
        var session = request.getSession();
        if(session != null && session.getAttribute(USERNAME_AATRIBUTE) != null){
            model.addAttribute(USERNAME_AATRIBUTE, session.getAttribute(USERNAME_AATRIBUTE));
            LOGGER.info(String.format(" Inside index  attributes: {}", session.getAttribute(ATTRIBUTES)));
            model.addAttribute(ATTRIBUTES, session.getAttribute(ATTRIBUTES));
            return "authenticated";
        } else {
            var loginUri = UriComponentsBuilder.fromHttpUrl(AUTHORIZATION_END_POINT)
                            .queryParam("redirect_uri", REDIRECT_URI)
                            .queryParam("response_type", "code")
                            .queryParam("state", UUID.randomUUID())
                            .queryParam("scope", "openid email profile")
                            .queryParam("client_id", clientId)
                            .build();
            model.addAttribute("loginUri", loginUri);
            return "anonymous";
        }
    }

    @GetMapping(AUTHORIZATION_SUCCESS_PATH)
    public String oauth2Callback(String code, HttpServletRequest request) throws  IOException{
        LOGGER.info("\n\n 🎁 code : "+code);

        var creds = getCredentials();
        var payload = new LinkedMultiValueMap<String, String>();
        payload.add("code", code);
        payload.add("grant_type", "authorization_code");
        payload.add("redirect_uri", REDIRECT_URI);
        var response = this.restClient.post()
                       .uri(TOKEN_URI)
                       .header("Authorization", "Basic "+ creds)
                       .body(payload)
                       .retrieve()
                       .body(String.class);
        var body = this.objectMapper.readValue(response, TokenResponse.class);
        LOGGER.info(body.idToken());
               
        var idToken = decodeIdToken(body.idToken());
        LOGGER.info(idToken.toString());
               
        var session = request.getSession(true);
        session.setAttribute(USERNAME_AATRIBUTE, idToken.get("email"));
        session.setAttribute(ATTRIBUTES, idToken);
        session.setAttribute(ACCESS_TOKEN, body.accessToken());

        return "redirect:/";
    }

    private String getCredentials() {
        var credsString = "%s:%s".formatted(clientId, clientSecret);
        return Base64.getUrlEncoder().encodeToString(credsString.getBytes());
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> decodeIdToken(String token) throws  IOException {
        var parts = token.split("\\.");
        var body = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readValue(body, Map.class);
    }

    record TokenResponse(
        @JsonProperty("id_token") String idToken,
        @JsonProperty(ACCESS_TOKEN) String accessToken
    ){

    }

    @GetMapping("/conferences")
    public String conferences(Model model, HttpServletRequest request){
        var session = request.getSession();
        if (session != null && session.getAttribute(ACCESS_TOKEN) != null) {
        try {
            var conferences = this.restClient.get()
                                    .uri("http://localhost:8081/conferences?userId")
                                    .header("authorization","Bearer " + session.getAttribute(ACCESS_TOKEN))
                                    .accept(MediaType.APPLICATION_JSON)   
                                    .retrieve()
                                    .body(List.class);
            
            model.addAttribute("conferences",conferences);

        } catch (Exception e) {
            model.addAttribute("error"," Error while getting conferencences : " + e.getMessage());
        }

        return "conferences";
    }
     return "redirect:/";
    }
    @PostMapping("/login")
    public String login(HttpServletRequest request){
        var session = request.getSession(true);
        session.setAttribute(USERNAME_AATRIBUTE, "daniel");
        request.getSession().setAttribute(ATTRIBUTES, 
        Map.of(
                "firstName", "daniel",
                "lastName", "Garnier",
                "company", "VMware",
                "type", "hardcoded"     
                )
        );

        return "redirect:/";


    }

    @PostMapping("/logout")
    public  String logout(HttpServletRequest request){
        var session = request.getSession();
       if ((session != null)) {
        session.invalidate();
       }
       return "redirect:/";
    }
}
