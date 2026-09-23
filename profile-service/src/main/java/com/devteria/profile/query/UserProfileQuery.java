package com.devteria.profile.query;

import static org.neo4j.cypherdsl.core.Cypher.anonParameter;
import static org.neo4j.cypherdsl.core.Cypher.noCondition;

import java.time.LocalDate;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserProfileQuery {
    public Condition buildCondition(String firstName, String lastName, String city, LocalDate dob) {
        Node userProfile = Cypher.node("user_profile").named("userProfile");
        Condition condition = noCondition();

        if (StringUtils.hasText(firstName)) {
            condition = condition.and(userProfile.property("firstName").isEqualTo(anonParameter(firstName)));
        }
        if (StringUtils.hasText(lastName)) {
            condition = condition.and(userProfile.property("lastName").isEqualTo(anonParameter(lastName)));
        }
        if (StringUtils.hasText(city)) {
            condition = condition.and(userProfile.property("city").isEqualTo(anonParameter(city)));
        }
        if (dob != null) {
            condition = condition.and(userProfile.property("dob").isEqualTo(anonParameter(dob)));
        }

        return condition;
    }
}
