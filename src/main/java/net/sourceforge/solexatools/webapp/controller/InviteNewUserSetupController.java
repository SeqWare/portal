package net.sourceforge.solexatools.webapp.controller;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.seqware.common.model.Registration;
import net.sourceforge.solexatools.Debug;
import net.sourceforge.solexatools.Security;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;

/**
 * <p>
 * InviteNewUserSetupController class.
 * </p>
 * 
 * @author boconnor
 * @version $Id: $Id
 */
public class InviteNewUserSetupController extends BaseCommandController {

    /**
     * <p>
     * Constructor for InviteNewUserSetupController.
     * </p>
     */
    public InviteNewUserSetupController() {
        super();
        setSupportedMethods(new String[] { METHOD_GET });
    }

    /**
     * {@inheritDoc}
     * 
     * @return
     * @throws java.lang.Exception
     */
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Registration registration = Security.getRegistration(request);

        ModelAndView modelAndView;
        HashMap<String, Object> model = new HashMap<>();

        Debug.put(": request.requestURI = " + request.getRequestURI());

        // ServletContext context = this.getServletContext();
        // Boolean isInvitationCode = Boolean.parseBoolean(context.getInitParameter("invitation.code"));

        if (registration == null || !registration.isLIMSAdmin() /* || !isInvitationCode */) {
            modelAndView = new ModelAndView("redirect:/login.htm");
        } else {
            /* assume they are updating their info */
            // model.put("strategy", "update");
            // request.setAttribute(getCommandName(), dto);
            // modelAndView = new ModelAndView("RegistrationUpdate", model);
            modelAndView = new ModelAndView("InviteNewUser");
        }
        return modelAndView;
    }
}
