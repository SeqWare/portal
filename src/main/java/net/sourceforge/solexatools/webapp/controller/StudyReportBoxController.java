package net.sourceforge.solexatools.webapp.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.seqware.common.business.FileReportService;
import net.sourceforge.seqware.common.business.SampleReportService;
import net.sourceforge.seqware.common.business.SampleReportService.Status;
import net.sourceforge.seqware.common.business.SampleService;
import net.sourceforge.seqware.common.business.StudyService;
import net.sourceforge.seqware.common.model.FileReportRow;
import net.sourceforge.seqware.common.model.Lane;
import net.sourceforge.seqware.common.model.LaneAttribute;
import net.sourceforge.seqware.common.model.Processing;
import net.sourceforge.seqware.common.model.Registration;
import net.sourceforge.seqware.common.model.Sample;
import net.sourceforge.seqware.common.model.SampleAttribute;
import net.sourceforge.seqware.common.model.SequencerRun;
import net.sourceforge.seqware.common.model.Study;
import net.sourceforge.seqware.common.model.Workflow;
import net.sourceforge.seqware.common.model.WorkflowRun;
import net.sourceforge.solexatools.Security;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;

/**
 * <p>
 * StudyReportBoxController class.
 * </p>
 * 
 * @author boconnor
 * @version $Id: $Id
 */
@SuppressWarnings("deprecation")
public class StudyReportBoxController extends BaseCommandController {

    /** Constant <code>STUDY_ID="study_id"</code> */
    public static final String STUDY_ID = "study_id";
    /** Constant <code>JSON="json"</code> */
    public static final String JSON = "json";
    /** Constant <code>SORT_NAME="sortname"</code> */
    public static final String SORT_NAME = "sortname";
    /** Constant <code>SORT_ORDER="sortorder"</code> */
    public static final String SORT_ORDER = "sortorder";
    /** Constant <code>CSV_TYPE="csvtype"</code> */
    public static final String CSV_TYPE = "csvtype";
    /** Constant <code>CHECK="check"</code> */
    public static final String CHECK = "check";

    private StudyService studyService;
    private SampleService sampleService;
    private FileReportService fileReportService;
    private SampleReportService sampleReportService;

    private boolean isSampleDownloaded;
    private boolean isFileDownloaded;

    /**
     * <p>
     * Setter for the field <code>studyService</code>.
     * </p>
     * 
     * @param studyService
     *            a {@link net.sourceforge.seqware.common.business.StudyService} object.
     */
    public void setStudyService(StudyService studyService) {
        this.studyService = studyService;
    }

    /**
     * <p>
     * Setter for the field <code>sampleService</code>.
     * </p>
     * 
     * @param service
     *            a {@link net.sourceforge.seqware.common.business.SampleService} object.
     */
    public void setSampleService(SampleService service) {
        this.sampleService = service;
    }

    /**
     * <p>
     * Setter for the field <code>fileReportService</code>.
     * </p>
     * 
     * @param service
     *            a {@link net.sourceforge.seqware.common.business.FileReportService} object.
     */
    public void setFileReportService(FileReportService service) {
        this.fileReportService = service;
    }

    /**
     * <p>
     * Setter for the field <code>sampleReportService</code>.
     * </p>
     * 
     * @param service
     *            a {@link net.sourceforge.seqware.common.business.SampleReportService} object.
     */
    public void setSampleReportService(SampleReportService service) {
        this.sampleReportService = service;
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
        if (registration == null) {
            return new ModelAndView("redirect:/login.htm");
        }

        boolean hasError = false;
        List<String> errMsgs = new ArrayList<>();

        // Get StudyId, for which Report is generated
        String idStr = request.getParameter(STUDY_ID);
        String csvtype = request.getParameter(CSV_TYPE);
        String check = request.getParameter(CHECK);

        int studyId = 0;

        try {
            studyId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            hasError = true;
            errMsgs.add(e.getMessage());
        }

        Study currentStudy = studyService.findByID(studyId);

        ModelAndView modelAndView = new ModelAndView("ReportStudy");

        if (!hasError || check != null) {
            if (csvtype != null) {
                // Sample CSV
                if (csvtype.equals("sample")) {
                    if (check != null) {
                        response.getWriter().write(Boolean.toString(isSampleDownloaded));
                        // reset
                        if (isSampleDownloaded) {
                            isSampleDownloaded = false;
                        }
                        response.flushBuffer();
                        return null;
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("Sample\tChild Sample\t");
                    List<Workflow> workflows = sampleReportService.getWorkflowsForStudy(currentStudy);
                    for (Workflow wf : workflows) {
                        sb.append(wf.getName()).append("\t");
                    }
                    sb.append("\n");
                    List<Sample> childSamples = sampleReportService.getChildSamples(currentStudy);
                    // Generate Rows
                    for (Sample sample : childSamples) {
                        Sample rootSample = sampleService.getRootSample(sample);
                        sb.append(rootSample.getTitle()).append("\t");
                        sb.append(rootSample.getSampleId() != sample.getSampleId() ? sample.getTitle() + "\t"
                                : "no child" + "\t");
                        for (Workflow workflow : workflows) {
                            Status status = sampleReportService.getStatus(currentStudy, sample, workflow);
                            if (status == null) {
                                sb.append(Status.notstarted).append("\t");
                            } else {
                                sb.append(status).append("\t");
                            }
                        }

                        sb.append("\n");
                    }

                    response.setContentType("text/csv");
                    response.addHeader("Content-Disposition", "attachment;filename=sampleReport.csv");
                    response.getWriter().write(sb.toString());
                    isSampleDownloaded = true;
                    response.flushBuffer();
                    return null;
                }

                // File CSV
                if (csvtype.equals("file")) {
                    if (check != null) {
                        response.getWriter().write(Boolean.toString(isFileDownloaded));
                        // reset
                        if (isFileDownloaded) {
                            isFileDownloaded = false;
                        }
                        response.flushBuffer();
                        return null;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("Study Title\tStudy SWID\tExperiment Name\tExperiment SWID\tParent Sample Name\t"
                            + "Parent Sample SWID\tParent Sample Attributes\t" + "Sample Name\tSample SWID\tSample Attributes\t"
                            + "Sequencer Run Name\tSequencer Run SWID\t" + "Lane Name\tLane Number\tLane SWID\tLane Attributes\t"
                            + "IUS Tag\tIUS SWID\t" + "Workflow Name\tWorkflow Version\tWorkflow SWID\t"
                            + "Workflow Run Name\t Workflow Run SWID\t" + "Processing Algorithm\tProcessing SWID\t"
                            + "File Meta-Type\tFile SWID\tFile Path\n");

                    List<FileReportRow> rows = fileReportService.getReportForStudy(currentStudy);

                    for (FileReportRow row : rows) {
                        sb.append(row.getStudy().getTitle()).append("\t");
                        sb.append(row.getStudy().getSwAccession()).append("\t");
                        sb.append(row.getExperiment().getName()).append("\t");
                        sb.append(row.getExperiment().getSwAccession()).append("\t");
                        sb.append(row.getSample().getName()).append("\t");
                        sb.append(row.getSample().getSwAccession()).append("\t");
                        StringBuffer attributes = new StringBuffer();
                        for (SampleAttribute attribute : row.getSample().getSampleAttributes()) {
                            attributes.append(attribute.getTag()).append("=").append(attribute.getValue()).append(" ");
                        }
                        sb.append(attributes.toString()).append("\t");
                        sb.append(row.getChildSample().getName()).append("\t");
                        sb.append(row.getChildSample().getSwAccession()).append("\t");
                        attributes = new StringBuffer();
                        for (SampleAttribute attribute : row.getChildSample().getSampleAttributes()) {
                            attributes.append(attribute.getTag()).append("=").append(attribute.getValue()).append(" ");
                        }
                        sb.append(attributes.toString()).append("\t");
                        Lane lane = row.getLane();
                        SequencerRun sequencerRun = null;
                        if (lane != null) {
                            sequencerRun = lane.getSequencerRun();
                        }
                        if (sequencerRun != null) {
                            sb.append(sequencerRun.getName()).append("\t");
                            sb.append(sequencerRun.getSwAccession()).append("\t");
                        } else {
                            sb.append(" \t");
                            sb.append(" \t");
                        }
                        if (lane != null) {
                            sb.append(lane.getName()).append("\t");
                            sb.append(lane.getLaneIndex()).append("\t");
                            sb.append(lane.getSwAccession()).append("\t");
                            attributes = new StringBuffer();
                            for (LaneAttribute attribute : lane.getLaneAttributes()) {
                                attributes.append(attribute.getTag()).append("=").append(attribute.getValue()).append(" ");
                            }
                            sb.append(attributes).append("\t");
                        } else {
                            sb.append(" \t");
                            sb.append(" \t");
                            sb.append(" \t");
                            sb.append(" \t");
                        }
                        sb.append(row.getIus().getTag()).append("\t");
                        sb.append(row.getIus().getSwAccession()).append("\t");
                        Processing processing = row.getProcessing();
                        WorkflowRun run = null;
                        Workflow workflow = null;
                        if (processing != null) {
                            run = processing.getWorkflowRun();
                            if (run == null) {
                                run = processing.getWorkflowRunByAncestorWorkflowRunId();
                            }
                        }
                        if (run != null) {
                            workflow = run.getWorkflow();

                        }
                        if (workflow != null) {
                            sb.append(workflow.getName()).append("\t");
                            sb.append(workflow.getVersion()).append("\t");
                            sb.append(workflow.getSwAccession()).append("\t");
                        } else {
                            sb.append(" \t");
                            sb.append(" \t");
                            sb.append(" \t");
                        }
                        if (run != null) {
                            sb.append(run.getName()).append("\t");
                            sb.append(run.getSwAccession()).append("\t");
                        } else {
                            sb.append(" \t");
                            sb.append(" \t");
                        }
                        if (processing != null) {
                            sb.append(processing.getAlgorithm()).append("\t");
                            sb.append(processing.getSwAccession()).append("\t");
                        } else {
                            sb.append(" \t");
                            sb.append(" \t");
                        }
                        sb.append(row.getFile().getMetaType()).append("\t");
                        sb.append(row.getFile().getSwAccession()).append("\t");
                        sb.append(row.getFile().getFilePath()).append("\t");
                        sb.append("\n");

                    }
                    response.setContentType("text/csv");
                    response.addHeader("Content-Disposition", "attachment;filename=fileReport.csv");
                    response.getWriter().write(sb.toString());
                    isFileDownloaded = true;
                    response.flushBuffer();
                    return null;
                }
            }

            if (currentStudy != null) {
                ModelAndView mv = new ModelAndView("ReportStudy");
                createChartModel(currentStudy, mv);
                mv.addObject("study_id", studyId);
                return mv;
            }
        }
        return modelAndView;
    }

    private void createChartModel(Study study, ModelAndView modelAndView) {
        createOverallChart(modelAndView, study);
        createWorkflowCharts(modelAndView, study);
    }

    private void createWorkflowCharts(ModelAndView modelAndView, Study study) {
        Map<Workflow, String> workflowCharts = new HashMap<>();
        List<Workflow> usedWorkflows = sampleReportService.getWorkflowsForStudy(study);
        for (Workflow workflow : usedWorkflows) {
            List<Status> statuses = sampleReportService.getStatusesForWorkflow(study, workflow);
            Map<Status, Integer> statusCount = new LinkedHashMap<>();
            statusCount.put(Status.failed, 0);
            statusCount.put(Status.pending, 0);
            statusCount.put(Status.running, 0);
            statusCount.put(Status.notstarted, 0);
            statusCount.put(Status.completed, 0);
            for (Status status : statuses) {
                int count = sampleReportService.countOfStatus(study, workflow, status);
                statusCount.put(status, count);
            }
            int current = 0;
            StringBuilder out = new StringBuilder();
            for (Status status : statusCount.keySet()) {
                current++;
                int count = statusCount.get(status);
                String sStatus = status.toString();
                if (Status.notstarted == status) {
                    sStatus = "not started";
                }
                out.append("['").append(sStatus).append("',").append(count).append("]");
                if (current != statusCount.keySet().size()) {
                    out.append(",");
                }
            }
            workflowCharts.put(workflow, out.toString());
        }
        modelAndView.addObject("names", usedWorkflows);
        modelAndView.addObject("chartData", workflowCharts);
    }

    private void createOverallChart(ModelAndView modelAndView, Study study) {
        List<Status> statuses = sampleReportService.getStatusesForStudy(study);
        Map<Status, Integer> statusCount = new LinkedHashMap<>();
        statusCount.put(Status.failed, 0);
        statusCount.put(Status.pending, 0);
        statusCount.put(Status.running, 0);
        statusCount.put(Status.notstarted, 0);
        statusCount.put(Status.completed, 0);
        for (Status status : statuses) {
            int count = sampleReportService.countOfStatus(study, status);
            statusCount.put(status, count);
        }
        int current = 0;
        StringBuilder out = new StringBuilder();
        for (Status status : statusCount.keySet()) {
            current++;
            int count = statusCount.get(status);
            String sStatus = status.toString();
            if (Status.notstarted == status) {
                sStatus = "not started";
            }
            out.append("['").append(sStatus).append("',").append(count).append("]");
            if (current != statusCount.keySet().size()) {
                out.append(",");
            }
        }
        modelAndView.addObject("overallChartData", out.toString());
    }

}
