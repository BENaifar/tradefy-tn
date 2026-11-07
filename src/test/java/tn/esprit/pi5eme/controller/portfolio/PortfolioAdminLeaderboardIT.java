package tn.esprit.pi5eme.controller.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.entity.Role;
import tn.esprit.pi5eme.entity.User;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.UserRepository;
import tn.esprit.pi5eme.security.UserDetailsImpl;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;
import tn.esprit.pi5eme.service.portfolio.PortfolioProvisioner;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioAdminLeaderboardIT {

    @MockBean IMarketDataService marketData;
    @MockBean MarketDataProvider provider;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PortfolioProvisioner provisioner;

    private User john;
    private User mary;

    private User newUser(String first, String last, String email, String cash) {
        User u = users.save(User.builder().firstName(first).lastName(last).email(email).password("x").build());
        Portfolio p = provisioner.getOrCreate(u.getId());
        p.setCashBalance(new BigDecimal(cash));
        return u;
    }

    private UserDetailsImpl principal(User u, Role role) {
        return new UserDetailsImpl(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), "x", role, true, true,
                List.of(new SimpleGrantedAuthority(role.name())));
    }

    @BeforeEach
    void setUp() {
        john = newUser("John", "Doe", "john.doe@leaderboard-test.tn", "110000.000");
        mary = newUser("Mary", "Smith", "mary.smith@leaderboard-test.tn", "90000.000");
    }

    @Test
    void adminListForbiddenForUser() throws Exception {
        mvc.perform(get("/api/portfolio/admin/portfolios").with(user(principal(john, Role.ROLE_USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListForAdmin() throws Exception {
        MvcResult r = mvc.perform(get("/api/portfolio/admin/portfolios").with(user(principal(mary, Role.ROLE_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.email=='john.doe@leaderboard-test.tn')].totalReturnPct").exists())
                .andReturn();
    }

    @Test
    void leaderboardOrderMeAndNoEmails() throws Exception {
        MvcResult r = mvc.perform(get("/api/portfolio/leaderboard").with(user(principal(mary, Role.ROLE_USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].displayName").value("John D."))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].isMe").value(false))
                .andExpect(jsonPath("$.data[1].displayName").value("Mary S."))
                .andExpect(jsonPath("$.data[1].isMe").value(true))
                .andReturn();
        assertFalse(r.getResponse().getContentAsString().contains("@"));
    }
}
