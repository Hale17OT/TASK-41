package com.dispatchops.web.controller;

import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class PageController {

    private static final Logger log = LoggerFactory.getLogger(PageController.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    @GetMapping("/login")
    public String loginPage() {
        log.debug("Serving login page");
        return "auth/login";
    }

    @GetMapping("/customer")
    public String customerPortal() {
        log.debug("Serving customer portal page");
        return "customer/portal";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) return "redirect:/login";
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        model.addAttribute("userId", user.getId());
        return "auth/change-password";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "dashboard/index";
    }

    @GetMapping("/fulfillment")
    public String fulfillment(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "fulfillment/board";
    }

    @GetMapping("/tasks")
    public String tasks(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "tasks/list";
    }

    @GetMapping("/credibility")
    public String credibility(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "credibility/panel";
    }

    @GetMapping("/contracts")
    public String contracts(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "contracts/list";
    }

    @GetMapping("/contracts/preview")
    public String contractPreview(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "contracts/preview";
    }

    @GetMapping("/contracts/sign")
    public String contractSign(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "contracts/sign";
    }

    @GetMapping("/tasks/calendar")
    public String taskCalendar(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "tasks/calendar";
    }

    @GetMapping("/payments")
    public String payments(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "payments/record";
    }

    @GetMapping("/search")
    public String search(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "search/results";
    }

    @GetMapping("/profile")
    public String profile(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "profile/edit";
    }

    @GetMapping("/notifications")
    public String notifications(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        return "notifications/center";
    }

    @GetMapping("/admin/users")
    public String adminUsers(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        if (!requireAdmin(request)) {
            return "redirect:/dashboard";
        }
        return "admin/users";
    }

    @GetMapping("/admin/regions")
    public String adminRegions(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        if (!requireAdmin(request)) {
            return "redirect:/dashboard";
        }
        return "admin/regions";
    }

    @GetMapping("/admin/settings")
    public String adminSettings(HttpServletRequest request, Model model) {
        if (!addCurrentUser(request, model)) {
            return "redirect:/login";
        }
        if (!requireAdmin(request)) {
            return "redirect:/dashboard";
        }
        return "admin/settings";
    }

    /**
     * Extracts the current user from the session and adds it to the model.
     *
     * @return true if a user was found in the session, false otherwise
     */
    private boolean addCurrentUser(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.debug("No session found, redirecting to login");
            return false;
        }

        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            log.debug("No user in session, redirecting to login");
            return false;
        }

        model.addAttribute("currentUser", user);

        // Serialize safe user subset as JSON for JS bootstrap (avoids HTML entity mangling from c:out)
        try {
            Map<String, Object> safeUser = new LinkedHashMap<>();
            safeUser.put("id", user.getId());
            safeUser.put("username", user.getUsername());
            safeUser.put("role", user.getRole() != null ? user.getRole().name() : null);
            safeUser.put("displayName", user.getDisplayName());
            model.addAttribute("currentUserJson", jsonMapper.writeValueAsString(safeUser));
        } catch (Exception e) {
            log.warn("Failed to serialize currentUserJson: {}", e.getMessage());
        }

        return true;
    }

    /**
     * Checks whether the current user has the ADMIN role.
     *
     * @return true if the user is an ADMIN, false otherwise (triggers redirect to dashboard)
     */
    private boolean requireAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        User user = (User) session.getAttribute("currentUser");
        if (user == null || user.getRole() != Role.ADMIN) {
            log.warn("Non-admin user '{}' attempted to access admin page",
                    user != null ? user.getUsername() : "unknown");
            return false;
        }
        return true;
    }
}
