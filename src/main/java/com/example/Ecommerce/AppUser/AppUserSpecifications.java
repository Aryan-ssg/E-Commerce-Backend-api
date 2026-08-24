package com.example.Ecommerce.AppUser;

import org.springframework.data.jpa.domain.Specification;

public class AppUserSpecifications {

    private AppUserSpecifications() {
      
    }

    public static Specification<AppUser> hasUsernameLike(String keyword) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("username")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<AppUser> hasRole(Role role) {
        return (root, query, cb) ->
                cb.equal(root.get("role"), role);
    }
}