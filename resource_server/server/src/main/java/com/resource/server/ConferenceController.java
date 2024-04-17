package com.resource.server;


import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ConferenceController {

    private final MultiValueMap<String,Conference> conferences ;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    

    public ConferenceController() {
        this.conferences = new LinkedMultiValueMap<>();
        this.conferences.add("alamnr@gmail.com", new Conference("Voxxed Days Zürich", "Zürich, Switzerland"));
        this.conferences.add("alamnr@gmail.com", new Conference("Voxxed Days Luxembourg", "Mondorf-les-Bains, Luxembourg"));
        this.conferences.add("alamnr@gmail.com", new Conference("RivieraDev", "Sophia Antipolis, France"));
        this.conferences.add("alamnr@gmail.com", new Conference("SpringOne", "Las Vegas, Nevada, USA"));
        this.conferences.add("alamnr@gmail.com", new Conference("Cloud Native Day", "Bern, Switzerland"));
        this.conferences.add("alamnr@gmail.com", new Conference("Devoxx", "Antwerp, Belgium"));
        this.conferences.add("alamnr@gmail.com", new Conference("J-Fall", "Ede, Netherlands"));

        this.conferences.add("-1", new Conference("Devoxx", "Antwerp, Belgium"));
        this.conferences.add("-1", new Conference("Fake Conferences", "Null Island, Somewhere"));

        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    @GetMapping("/conferences")
    public List<Conference> conferences(HttpServletRequest request) throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        var authJwt = request.getHeader("authorization").replace("Bearer ", "");
        var decodedToken = decodeJwt(authJwt);
        var keys = JwkUtils.getKeys("https://dev-47658814.okta.com/oauth2/default/v1/keys");
        var kid = decodedToken.header().kid();
        var publicKey = keys.stream().filter(jwk -> jwk.kid().equals(kid))
                .findFirst()
                .get()
                .publicKey();
        var isValid = JwkUtils.verifySignature(publicKey, decodedToken.signedData, decodedToken.signature);
        if (!isValid) {
            throw new RuntimeException("invalid access token");
        }
        var expiry = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(decodedToken.payload().exp()),
                ZoneId.systemDefault()
        );
        if (expiry.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Expired access_token");
        }
        // if (!decodedToken.payload().scopes().contains("conference.list")) {
        //     throw new RuntimeException("Missing scope: conference.list");
        // }

        var userId = decodedToken.payload().sub();

        return this.conferences.get(userId);
}

record Conference(String name, String location) {
}


private Jwt decodeJwt(String token) throws IOException {
    var parts = token.split("\\.");
    return new Jwt(
            objectMapper.readValue(Base64.getUrlDecoder().decode(parts[0]), Header.class),
            objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Payload.class),
            token.substring(0, token.lastIndexOf(".")).getBytes(),
            Base64.getUrlDecoder().decode(parts[2])
    );
}

record Jwt(Header header, Payload payload, byte[] signedData, byte[] signature) {
}

record Payload(String sub, @JsonProperty("scp") List<String> scopes, Long exp) {
}

record Header(String kid) {
}

}
