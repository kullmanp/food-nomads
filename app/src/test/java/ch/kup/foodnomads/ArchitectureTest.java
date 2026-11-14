package ch.kup.foodnomads;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

/** The layering of ADR-0003 and the repository seam of ADR-0005. */
class ArchitectureTest {

    private static final String API = "ch.kup.foodnomads.api..";
    private static final String WEB = "ch.kup.foodnomads.web..";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("ch.kup.foodnomads");

    private static final DescribedPredicate<JavaMethodCall> PANACHE_ENTITY_OPERATION = DescribedPredicate.describe(
            "a Panache entity operation",
            call -> call.getTarget()
                    .resolveMember()
                    .filter(method -> method.getOwner().isEquivalentTo(PanacheEntityBase.class))
                    .isPresent());

    @Test
    void applicationLayerDoesNotKnowTheAdapters() {
        noClasses()
                .that()
                .resideOutsideOfPackages(API, WEB)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(API, WEB)
                .because("business rules live in the shared application layer (ADR-0003)")
                .check(CLASSES);
    }

    @Test
    void pagesDoNotKnowTheApi() {
        noClasses()
                .that()
                .resideInAPackage(WEB)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(API)
                .check(CLASSES);
    }

    @Test
    void apiDoesNotKnowThePages() {
        noClasses()
                .that()
                .resideInAPackage(API)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(WEB)
                .check(CLASSES);
    }

    @Test
    void venuesDoNotKnowTheLunches() {
        noClasses()
                .that()
                .resideInAPackage("ch.kup.foodnomads.venues..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("ch.kup.foodnomads.lunches..")
                .because("lunches depend on venues, never the reverse: visited is derived from the Lunches")
                .check(CLASSES);
    }

    @Test
    void adaptersDoNotUseRepositories() {
        noClasses()
                .that()
                .resideInAnyPackage(API, WEB)
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("adapters go through the application layer (ADR-0003)")
                .check(CLASSES);
    }

    @Test
    void onlyPanacheRepositoriesAndEntitiesTouchPersistence() {
        noClasses()
                .that()
                .haveSimpleNameNotStartingWith("Panache")
                .and()
                .areNotAnnotatedWith(Entity.class)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.quarkus.hibernate..", "jakarta.persistence..", "org.hibernate..")
                .because("persistence goes behind a repository interface (ADR-0005)")
                .check(CLASSES);
    }

    @Test
    void onlyPanacheRepositoriesRunEntityOperations() {
        noClasses()
                .that()
                .haveSimpleNameNotStartingWith("Panache")
                .should()
                .callMethodWhere(PANACHE_ENTITY_OPERATION)
                .because("persistence goes behind a repository interface (ADR-0005)")
                .check(CLASSES);
    }
}
