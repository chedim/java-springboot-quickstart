package org.couchbase.quickstart.userProfile;

import com.couchbase.client.core.error.CollectionExistsException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.core.error.IndexExistsException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.collection.CollectionManager;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.query.QueryResult;
import org.couchbase.quickstart.configs.CollectionNames;
import org.couchbase.quickstart.configs.DBProperties;
import org.couchbase.quickstart.models.Profile;
import org.couchbase.quickstart.models.ProfileResult;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class UserProfileTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private Cluster cluster;
    @Autowired
    private Bucket bucket;
    @Autowired
    private DBProperties prop;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();


    @Before
    public void init() {
                try {
            cluster.queryIndexes().createPrimaryIndex(prop.getBucketName());
        } catch (Exception e) {
            System.out.println("Primary index already exists on bucket "+prop.getBucketName());
        }

        CollectionManager collectionManager = bucket.collections();
        try {
            CollectionSpec spec = CollectionSpec.create(CollectionNames.PROFILE, bucket.defaultScope().name());
            collectionManager.createCollection(spec);
        } catch (CollectionExistsException e){
            System.out.println(String.format("Collection <%s> already exists", CollectionNames.PROFILE));
        } catch (Exception e) {
            System.out.println(String.format("Generic error <%s>",e.getMessage()));
        }

        try {
            final QueryResult result = cluster.query("CREATE PRIMARY INDEX default_profile_index ON "+prop.getBucketName()+"._default."+ CollectionNames.PROFILE);
            for (JsonObject row : result.rowsAsObject()){
                System.out.println(String.format("Index Creation Status %s",row.getObject("meta").getString("status")));
            }
        } catch (IndexExistsException e){
            System.out.println(String.format("Collection's primary index already exists"));
        } catch (Exception e){
            System.out.println(String.format("General error <%s> when trying to create index ",e.getMessage()));
        }

        cluster.query("DELETE FROM "+prop.getBucketName()+"._default.profile ");
    }
    
    @AfterEach
    public void cleanDB() {
        cluster.query("DELETE FROM "+prop.getBucketName()+"._default.profile ");
    }

    @Test
    public void testUserProfileNotFound() {

        this.webTestClient.get()
                .uri("/api/v1/profiles?limit=5&skip=0&searchFirstName=Bob")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCreateUserProfile(){
        //test data
        Profile testProfile = getTestProfile();
        String json = getCreatedUserJson(testProfile);

        //run the post test
        EntityExchangeResult<ProfileResult> profileResult = this.webTestClient.post()
                .uri("/api/v1/profiles/")
                .bodyValue(json)
                .accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", "application/json; charset=utf-8")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProfileResult.class)
                .returnResult();

        Profile result = bucket.collection(CollectionNames.PROFILE)
                .get(profileResult.getResponseBody().getPid())
                .contentAs(Profile.class);

        assertEquals(result.getFirstName(), testProfile.getFirstName());
        assertEquals(result.getLastName(), testProfile.getLastName());
        assertEquals(result.getEmail(), testProfile.getEmail());
        assertNotEquals(result.getPassword(), testProfile.getPassword());
        assertNotNull(result.getPid());
    }


    @Test
    public void testListUsersSuccess() {

        //test data
        Profile testProfile = getTestProfile();
        bucket.collection(CollectionNames.PROFILE).insert(testProfile.getPid(), testProfile);

        EntityExchangeResult<List<Profile>> profileListResult = this.webTestClient.get()
            .uri("/api/v1/profiles/?limit=5&skip=0&searchFirstName=Jam")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(Profile.class)
            .returnResult();

        MatcherAssert.assertThat(profileListResult.getResponseBody(), Matchers.hasSize(1));
        Profile result = profileListResult.getResponseBody().get(0);

        assertEquals(result.getFirstName(), testProfile.getFirstName());
        assertEquals(result.getLastName(), testProfile.getLastName());
        assertEquals(result.getEmail(), testProfile.getEmail());
        assertNotEquals(result.getPassword(), testProfile.getPassword());
        assertNotNull(result.getPid());
    }


    @Test
    public void testListUsersNoResult() {

        //test data
        Profile testProfile = getTestProfile();
        bucket.collection(CollectionNames.PROFILE).insert(testProfile.getPid(), testProfile);

        EntityExchangeResult<List<Profile>> profileListResult = this.webTestClient.get()
            .uri("/api/v1/profiles/?limit=5&skip=0&searchFirstName=Jack")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(Profile.class)
            .returnResult();

        MatcherAssert.assertThat(profileListResult.getResponseBody(), Matchers.hasSize(0));
    }

    @Test
    public void testDeleteUserProfile() {

        exceptionRule.expect(DocumentNotFoundException.class);
        exceptionRule.expectMessage("Document with the given id not found");

        //test data
        Profile testProfile = getTestProfile();
        bucket.collection(CollectionNames.PROFILE).insert(testProfile.getPid(), testProfile);

        //delete the user
        this.webTestClient.delete()
                .uri(String.format("/api/v1/profiles/%s", testProfile.getPid()))
                .accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", "application/json; charset=utf-8")
                .exchange()
                .expectStatus().isOk();

        bucket.collection(CollectionNames.PROFILE).get(testProfile.getPid());
    }

    private String getCreatedUserJson(Profile profile) {
        //create json to post to integration test
        return JsonObject.create()
                .put("firstName", profile.getFirstName())
                .put("lastName", profile.getLastName())
                .put("password", profile.getPassword())
                .put("email", profile.getEmail()).toString();
    }

    private Profile getTestProfile() {
        return new Profile(
                UUID.randomUUID().toString(),
                "James",
                "Gosling",
                "password",
                "james.gosling@sun.com");
    }
}
