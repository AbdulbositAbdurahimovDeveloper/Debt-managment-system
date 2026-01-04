package uz.qarzdorlar_ai.specification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.model.UserProfile;
import uz.qarzdorlar_ai.payload.UserFilterDTO;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> filter(UserFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // UserProfile bilan join qilish (chunki firstName, lastName, phoneNumber o'sha yerda)
            // JoinType.LEFT ishlatamiz, agar profili bo'lmasa ham user chiqaversin
            Join<User, UserProfile> profileJoin = root.join(User.Fields.userProfile, JoinType.LEFT);

            // 1. GLOBAL SEARCH (search field to'ldirilgan bo'lsa)
            if (StringUtils.hasText(filter.getSearch())) {
                String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                
                Predicate globalPredicate = cb.or(
                        cb.like(cb.lower(root.get(User.Fields.username)), pattern),
                        cb.like(cb.lower(profileJoin.get(UserProfile.Fields.firstName)), pattern),
                        cb.like(cb.lower(profileJoin.get(UserProfile.Fields.lastName)), pattern),
                        cb.like(cb.lower(profileJoin.get(UserProfile.Fields.phoneNumber)), pattern)
                );
                predicates.add(globalPredicate);
            }

            // 2. SPECIFIC FIELD FILTERS (AND operatori bilan bog'lanadi)
            
            if (StringUtils.hasText(filter.getUsername())) {
                predicates.add(cb.like(cb.lower(root.get(User.Fields.username)), 
                        "%" + filter.getUsername().toLowerCase() + "%"));
            }

            if (filter.getRole() != null) {
                predicates.add(cb.equal(root.get(User.Fields.role), filter.getRole()));
            }

            if (StringUtils.hasText(filter.getFirstName())) {
                predicates.add(cb.like(cb.lower(profileJoin.get(UserProfile.Fields.firstName)), 
                        "%" + filter.getFirstName().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(filter.getLastName())) {
                predicates.add(cb.like(cb.lower(profileJoin.get(UserProfile.Fields.lastName)), 
                        "%" + filter.getLastName().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(filter.getPhoneNumber())) {
                predicates.add(cb.like(profileJoin.get(UserProfile.Fields.phoneNumber), 
                        "%" + filter.getPhoneNumber() + "%"));
            }

            if (filter.getEmailEnabled() != null) {
                predicates.add(cb.equal(profileJoin.get(UserProfile.Fields.emailEnabled), filter.getEmailEnabled()));
            }

            // Natijalarni birlashtirish (Hamma shartlar AND bilan bog'lanadi)
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}