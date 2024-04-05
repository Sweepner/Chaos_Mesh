package pl.rodzon.endpoints.users.repository;

import org.springframework.data.jpa.domain.Specification;
import pl.rodzon.endpoints.users.model.UserDAO;

public class UsersSpec {
    private final Specification<UserDAO> specification;

    public UsersSpec(UsersFilter usersFilter) {
        Specification<UserDAO> usersDAOSpecification = (r, q, b) -> {
            if(usersFilter.getUsername() != null) {
                return b.like(r.get("username"), "%" + usersFilter.getUsername() + "%");
            }
            return null;
        };
        this.specification = Specification
                .where(usersDAOSpecification);
    }

    public Specification<UserDAO> getSpecification() {
        return this.specification;
    }
}
