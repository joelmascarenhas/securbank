/**
 * 
 */
package securbank.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import securbank.models.ModificationRequest;
import securbank.models.User;
import securbank.services.UserService;
import securbank.validators.InternalEditUserFormValidator;

/**
 * @author Ayush Gupta
 *
 */
@Controller
public class EmployeeController {
	@Autowired
	private UserService userService;
	
	@Autowired
	private InternalEditUserFormValidator editUserFormValidator;
	
	@GetMapping("/employee/details")
    public String currentUserDetails(Model model) {
		User user = userService.getCurrentUser();
		if (user == null) {
			return "redirect:/error?code=user-notfound";
		}
		
		model.addAttribute("user", user);
		
        return "employee/detail";
    }
	
	@GetMapping("/employee/edit")
    public String editUser(Model model) {
		User user = userService.getCurrentUser();
		if (user == null) {
			return "redirect:/error";
		}
		model.addAttribute("user", user);
		
        return "employee/edit";
    }
	
	@PostMapping("/employee/edit")
    public String editSubmit(@ModelAttribute ModificationRequest request, BindingResult bindingResult) {
		editUserFormValidator.validate(request, bindingResult);
		if (bindingResult.hasErrors()) {
			return "employee/edit";
        }
		
		// create request
    	userService.createInternalModificationRequest(request);
	
        return "redirect:/";
    }
	
	@GetMapping("/employee/user/request")
    public String getAllUserRequest(Model model) {
		List<ModificationRequest> modificationRequests = userService.getModificationRequests("pending", "external");
		if (modificationRequests == null) {
			model.addAttribute("modificationrequests", new ArrayList<ModificationRequest>());
		}
		else {
			model.addAttribute("modificationrequests", modificationRequests);	
		}
		
        return "employee/modificationrequests";
    }
	
	@GetMapping("/employee/user/request/{id}")
    public String getUserRequest(Model model, @PathVariable() UUID id) {
		ModificationRequest modificationRequest = userService.getModificationRequest(id);
		
		if (modificationRequest == null) {
			return "redirect:/error?=code=400&path=request-invalid";
		}
		
		model.addAttribute("modificationrequest", modificationRequest);
    	
        return "employee/modificationrequest_detail";
    }
	
	@PostMapping("/employee/user/request/{requestId}")
    public String approveEdit(@PathVariable UUID requestId, @ModelAttribute ModificationRequest request, BindingResult bindingResult) {
		String status = request.getStatus();
		if (status == null || !(request.getStatus().equals("approved") || !request.getStatus().equals("rejected"))) {
			return "redirect:/error?code=400&path=request-action-invalid";
		}
		request = userService.getModificationRequest(requestId);
		
		// checks validity of request
		if (request == null) {
			return "redirect:/error?code=400&path=request-invalid";
		}
		
		// checks if employee is authorized for the request to approve
		if (!request.getUserType().equals("external")) {
			return "redirect:/error?code=401&path=request-unauthorised";
		}
		request.setStatus(status);
		if (status.equals("approved")) {
			userService.approveModificationRequest(request);
		}
		// rejects request
		else {
			userService.rejectModificationRequest(request);
		}
		
        return "redirect:/employee/user/request";
    }
}
