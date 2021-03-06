package net.sourceforge.solexatools.webapp.controller; // -*- tab-width: 4 -*-

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.seqware.common.business.PlatformService;
import net.sourceforge.seqware.common.business.SequencerRunService;
import net.sourceforge.seqware.common.model.Registration;
import net.sourceforge.seqware.common.model.SequencerRun;
import net.sourceforge.seqware.common.model.SequencerRunWizardDTO;
import net.sourceforge.solexatools.Debug;
import net.sourceforge.solexatools.Security;
import net.sourceforge.solexatools.util.SetNodeIdInSession;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;

/**
 * RegistrationSetupController This is invoked upon entry to Registration.jsp or RegistrationUpdate.jsp
 * 
 * @author boconnor
 * @version $Id: $Id
 */
public class SequencerRunWizardSetupController extends BaseCommandController {

    SequencerRunService sequencerRunService = null;
    private PlatformService platformService;

    /**
     * <p>
     * Constructor for SequencerRunWizardSetupController.
     * </p>
     */
    public SequencerRunWizardSetupController() {
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
        if (registration == null) return new ModelAndView("redirect:/login.htm");

        ModelAndView modelAndView = null;
        HashMap<String, Object> model = new HashMap<>();
        SequencerRun sequencerRun = getRequestedSequencerRun(request);
        boolean isReport = request.getParameter("report") != null;

        model.put("platformList", getPlatformService().list(registration));

        Debug.put(": request.requestURI = " + request.getRequestURI());
        Debug.put("SequencerRunWizardSetupController: command name is " + getCommandName());
        if (sequencerRun != null) {
            // LEFT OFF WITH: need to populate the platform ID
            if (sequencerRun.getPlatform() != null) {
                sequencerRun.setPlatformInt(sequencerRun.getPlatform().getPlatformId());
            }
            request.setAttribute(getCommandName(), sequencerRun);
            request.setAttribute("swid", sequencerRun.getSwAccession());
            model.put("strategy", "update");
            if (!isReport) {
                modelAndView = new ModelAndView("SequencerRunWizard", model);
            } else {
                modelAndView = new ModelAndView("SequencerRunReport", model);
            }
        } else {
            request.setAttribute(getCommandName(), new SequencerRunWizardDTO());
            model.put("strategy", "submit");
            // I think this is referring to the JSP to use!
            modelAndView = new ModelAndView("SequencerRunWizard", model);
        }
        // remove id
        SetNodeIdInSession.removeSequencerRun(request);
        return modelAndView;
    }

    private SequencerRun getRequestedSequencerRun(HttpServletRequest request) {
        SequencerRun sequencerRun = null;
        HttpSession session = request.getSession(false);
        // Study study = null;
        String id = (String) request.getParameter("sequencerRunId");
        session.removeAttribute("sequencerRun");

        if (id != null && !"".equals(id)) {
            sequencerRun = sequencerRunService.findByID(Integer.parseInt(id));
            // sequencerRun = (SequencerRun)session.getAttribute("sequencerRun");
            session.setAttribute("sequencerRun", sequencerRun);
        }
        return (sequencerRun);
    }

    /**
     * <p>
     * Getter for the field <code>sequencerRunService</code>.
     * </p>
     * 
     * @return a {@link net.sourceforge.seqware.common.business.SequencerRunService} object.
     */
    public SequencerRunService getSequencerRunService() {
        return sequencerRunService;
    }

    /**
     * <p>
     * Setter for the field <code>sequencerRunService</code>.
     * </p>
     * 
     * @param sequencerRunService
     *            a {@link net.sourceforge.seqware.common.business.SequencerRunService} object.
     */
    public void setSequencerRunService(SequencerRunService sequencerRunService) {
        this.sequencerRunService = sequencerRunService;
    }

    /**
     * <p>
     * Getter for the field <code>platformService</code>.
     * </p>
     * 
     * @return a {@link net.sourceforge.seqware.common.business.PlatformService} object.
     */
    public PlatformService getPlatformService() {
        return platformService;
    }

    /**
     * <p>
     * Setter for the field <code>platformService</code>.
     * </p>
     * 
     * @param platformService
     *            a {@link net.sourceforge.seqware.common.business.PlatformService} object.
     */
    public void setPlatformService(PlatformService platformService) {
        this.platformService = platformService;
    }
}

// ex:sw=4:ts=4:
