package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PropertyNameConstraintRepository extends JpaRepository<PropertyNameConstraintEntity, String> {

    @Query(value = "SELECT CAST(CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END AS BIT) FROM property_name_constraint WHERE property_name = :name", nativeQuery = true)
    boolean nameExists(@Param("name") String propertyName);

    @Query(value = "SELECT CAST(CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END AS BIT) FROM property_name_constraint WHERE property_name = :name AND property_id != :id", nativeQuery = true)
    boolean nameExistsForAnotherProperty(@Param("id") String propertyId, @Param("name") String propertyName);

    Optional<PropertyNameConstraintEntity> findByPropertyName(String propertyName);
}
