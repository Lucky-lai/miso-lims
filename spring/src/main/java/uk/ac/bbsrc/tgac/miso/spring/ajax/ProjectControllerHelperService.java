/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey, Mario Caccamo @ TGAC
 * *********************************************************************
 *
 * This file is part of MISO.
 *
 * MISO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MISO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO.  If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package uk.ac.bbsrc.tgac.miso.spring.ajax;

import static uk.ac.bbsrc.tgac.miso.core.util.LimsUtils.isStringEmptyOrNull;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import com.eaglegenomics.simlims.core.Note;
import com.eaglegenomics.simlims.core.User;
import com.eaglegenomics.simlims.core.manager.SecurityManager;
import com.google.json.JsonSanitizer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.Ajaxified;
import net.sourceforge.fluxion.ajax.util.JSONUtils;
import uk.ac.bbsrc.tgac.miso.core.data.Barcodable;
import uk.ac.bbsrc.tgac.miso.core.data.Dilution;
import uk.ac.bbsrc.tgac.miso.core.data.EntityGroup;
import uk.ac.bbsrc.tgac.miso.core.data.Library;
import uk.ac.bbsrc.tgac.miso.core.data.Pool;
import uk.ac.bbsrc.tgac.miso.core.data.PrintJob;
import uk.ac.bbsrc.tgac.miso.core.data.Project;
import uk.ac.bbsrc.tgac.miso.core.data.Run;
import uk.ac.bbsrc.tgac.miso.core.data.Sample;
import uk.ac.bbsrc.tgac.miso.core.data.impl.EntityGroupImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryDilution;
import uk.ac.bbsrc.tgac.miso.core.data.impl.ProjectOverview;
import uk.ac.bbsrc.tgac.miso.core.data.impl.SampleImpl;
import uk.ac.bbsrc.tgac.miso.core.event.manager.WatchManager;
import uk.ac.bbsrc.tgac.miso.core.exception.MisoPrintException;
import uk.ac.bbsrc.tgac.miso.core.manager.IssueTrackerManager;
import uk.ac.bbsrc.tgac.miso.core.manager.MisoFilesManager;
import uk.ac.bbsrc.tgac.miso.core.manager.PrintManager;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;
import uk.ac.bbsrc.tgac.miso.core.service.printing.MisoPrintService;
import uk.ac.bbsrc.tgac.miso.core.service.printing.context.PrintContext;
import uk.ac.bbsrc.tgac.miso.core.util.FormUtils;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;

/**
 * uk.ac.bbsrc.tgac.miso.spring.ajax
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @since 0.0.2
 */
@Ajaxified
public class ProjectControllerHelperService {
  protected static final Logger log = LoggerFactory.getLogger(ProjectControllerHelperService.class);
  @Autowired
  private SecurityManager securityManager;
  @Autowired
  private RequestManager requestManager;
  @Autowired
  private IssueTrackerManager issueTrackerManager;
  @Autowired
  private PrintManager<MisoPrintService, Queue<?>> printManager;
  @Autowired
  private MisoFilesManager misoFileManager;
  @Autowired
  private WatchManager watchManager;

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  public void setIssueTrackerManager(IssueTrackerManager issueTrackerManager) {
    this.issueTrackerManager = issueTrackerManager;
  }

  public void setMisoFileManager(MisoFilesManager misoFileManager) {
    this.misoFileManager = misoFileManager;
  }

  public void setPrintManager(PrintManager<MisoPrintService, Queue<?>> printManager) {
    this.printManager = printManager;
  }

  public void setWatchManager(WatchManager watchManager) {
    this.watchManager = watchManager;
  }

  public JSONObject addProjectOverview(HttpSession session, JSONObject json) {
    final Long projectId = json.getLong("projectId");
    final String principalInvestigator = (String) json.get("principalInvestigator");
    final Integer numProposedSamples = json.getInt("numProposedSamples");

    try {
      final Project project = requestManager.getProjectById(projectId);
      final ProjectOverview overview = new ProjectOverview();
      overview.setNumProposedSamples(numProposedSamples);
      overview.setPrincipalInvestigator(principalInvestigator);
      overview.setProject(project);
      overview.setLocked(false);
      project.getOverviews().add(overview);
      requestManager.saveProjectOverview(overview);
      requestManager.saveProject(project);
    } catch (final IOException e) {
      log.error("add project overview", e);
      return JSONUtils.SimpleJSONError(e.getMessage());
    }

    return JSONUtils.SimpleJSONResponse("ok");
  }

  public JSONObject addProjectOverviewNote(HttpSession session, JSONObject json) {
    final Long overviewId = json.getLong("overviewId");
    String internalOnly = json.getString("internalOnly");
    final String text = json.getString("text");

    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      final ProjectOverview overview = requestManager.getProjectOverviewById(overviewId);
      final Project project = overview.getProject();

      final Note note = new Note();

      internalOnly = internalOnly.equals("on") ? "true" : "false";

      note.setInternalOnly(Boolean.parseBoolean(internalOnly));
      note.setText(text);
      note.setOwner(user);
      note.setCreationDate(new Date());
      overview.getNotes().add(note);
      requestManager.saveProjectOverviewNote(overview, note);
      requestManager.saveProject(project);
    } catch (final IOException e) {
      log.error("add project overview note", e);
      return JSONUtils.SimpleJSONError(e.getMessage());
    }

    return JSONUtils.SimpleJSONResponse("ok");
  }

  public JSONObject deleteProjectOverviewNote(HttpSession session, JSONObject json) {
    final Long overviewId = json.getLong("overviewId");
    final Long noteId = json.getLong("noteId");

    try {
      final ProjectOverview po = requestManager.getProjectOverviewById(overviewId);
      final Note note = requestManager.getNoteById(noteId);
      if (po.getNotes().contains(note)) {
        po.getNotes().remove(note);
        requestManager.deleteNote(note);
        requestManager.saveProjectOverview(po);
        return JSONUtils.SimpleJSONResponse("OK");
      } else {
        return JSONUtils.SimpleJSONError("Project Overview does not have note " + noteId + ". Cannot remove");
      }
    } catch (final IOException e) {
      log.error("delete project overview", e);
      return JSONUtils.SimpleJSONError("Cannot remove note: " + e.getMessage());
    }
  }

  public JSONObject deleteProjectFile(HttpSession session, JSONObject json) {
    final Long id = json.getLong("id");
    final Integer hashcode = json.getInt("hashcode");
    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      final Project project = requestManager.getProjectById(id);

      if (project.userCanWrite(user)) {
        String filename = null;
        for (final String s : misoFileManager.getFileNames(Project.class, id.toString())) {
          if (s.hashCode() == hashcode) {
            filename = s;
            break;
          }
        }
        log.info(MessageFormat.format("Attempting to delete file {0}", filename));
        misoFileManager.deleteFile(Project.class, id.toString(), filename);
        log.info(MessageFormat.format("{0} deleted", filename));
        return JSONUtils.SimpleJSONResponse("OK");
      } else {
        return JSONUtils.SimpleJSONError(MessageFormat.format("Cannot delete file id {0}.  Access denied.", id));
      }
    } catch (final IOException e) {
      log.error("delete project file", e);
      return JSONUtils.SimpleJSONError("Cannot remove file: " + e.getMessage());
    }
  }

  public JSONObject unlockProjectOverview(HttpSession session, JSONObject json) {
    final Long overviewId = json.getLong("overviewId");
    try {
      final ProjectOverview overview = requestManager.getProjectOverviewById(overviewId);
      overview.setLocked(false);
      requestManager.saveProjectOverview(overview);
      requestManager.saveProject(overview.getProject());
    } catch (final IOException e) {
      log.error("unlock project overview", e);
      return JSONUtils.SimpleJSONError(e.getMessage());
    }
    return JSONUtils.SimpleJSONResponse("ok");
  }

  public JSONObject lockProjectOverview(HttpSession session, JSONObject json) {
    final Long overviewId = json.getLong("overviewId");
    try {
      final ProjectOverview overview = requestManager.getProjectOverviewById(overviewId);
      overview.setLocked(true);
      requestManager.saveProjectOverview(overview);
      requestManager.saveProject(overview.getProject());
    } catch (final IOException e) {
      log.error("lock project overview", e);
      return JSONUtils.SimpleJSONError(e.getMessage());
    }
    return JSONUtils.SimpleJSONResponse("ok");
  }

  public JSONObject previewIssues(HttpSession session, JSONObject json) {
    if (issueTrackerManager != null) {
      final List<JSONObject> issueList = new ArrayList<JSONObject>();
      final List<String> errorList = new ArrayList<String>();
      final JSONArray issues = JSONArray.fromObject(json.getString("issues"));
      for (final JSONObject issueKey : (Iterable<JSONObject>) issues) {
        JSONObject issue = null;
        try {
          issue = issueTrackerManager.getIssue(issueKey.getString("key"));
          if (issue != null) {
            issueList.add(issue);
          } else {
            errorList.add(issueKey.getString("key"));
          }
        } catch (final IOException e) {
          log.error("preview issues", e);
          errorList.add(issueKey.getString("key"));
        }
      }

      final JSONObject j = new JSONObject();
      j.put("validIssues", JSONArray.fromObject(issueList));
      j.put("invalidIssues", JSONArray.fromObject(errorList));
      return j;
    } else {
      return JSONUtils.SimpleJSONError("No issue tracker manager available.");
    }
  }

  public JSONObject getIssues(HttpSession session, JSONObject json) {
    if (issueTrackerManager != null) {
      final Long projectId = json.getLong("projectId");
      try {
        final Project project = requestManager.getProjectById(projectId);
        final JSONObject j = new JSONObject();
        if (project != null) {
          final List<JSONObject> issueList = new ArrayList<JSONObject>();

          if (project.getIssueKeys() != null) {
            for (final String issueKey : project.getIssueKeys()) {
              final JSONObject issue = issueTrackerManager.getIssue(issueKey);
              if (issue != null) {
                issueList.add(issue);
              }
            }
            j.put("issues", JSONArray.fromObject(issueList));
          }
        }
        return j;
      } catch (final IOException e) {
        log.error("get issues", e);
        return JSONUtils.SimpleJSONError(e.getMessage());
      }
    } else {
      return JSONUtils.SimpleJSONError("No issue tracker manager available.");
    }
  }

  public JSONObject listProjectTrafficLight(HttpSession session, JSONObject json) {
    try {
      final JSONObject j = new JSONObject();
      for (final Project project : requestManager.listAllProjects()) {
        j.put(project.getProjectId(), checkOverviews(project.getProjectId()));
      }
      return j;
    } catch (final IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public JSONObject checkOverviewByProjectId(HttpSession session, JSONObject json) {
    try {
      final JSONObject j = new JSONObject();
      final Long projectId = json.getLong("projectId");
      j.put("response", checkOverviews(projectId));
      return j;
    } catch (final IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  private String checkOverviews(Long projectId) throws IOException {
    final StringBuilder sb = new StringBuilder();
    final Collection<ProjectOverview> overviews = requestManager.listAllOverviewsByProjectId(projectId);
    if (overviews.size() > 0) {
      sb.append("<table class=\"overviewSummary\">");
      for (final ProjectOverview overview : overviews) {
        sb.append("<tr>");
        sb.append("<td class=\"smallbox" + colorOverviewState(overview.getAllSampleQcPassed()) + "\">SQ</td>");
        sb.append("<td class=\"smallbox" + colorOverviewState(overview.getLibraryPreparationComplete()) + "\">L</td>");
        sb.append("<td class=\"smallbox" + colorOverviewState(overview.getAllLibrariesQcPassed()) + "\">LQ</td>");
        sb.append("<td class=\"smallbox" + colorOverviewState(overview.getAllPoolsConstructed()) + "\">P</td>");
        sb.append("<td class=\"smallbox" + colorOverviewState(overview.getAllRunsCompleted()) + "\">R</td>");
        sb.append("<td class=\"smallbox" + colorOverviewState(overview.getPrimaryAnalysisCompleted()) + "\">A</td>");
        sb.append("</tr>");
      }
      sb.append("</table>");
    } else {
      sb.append("No Overview");
    }
    return sb.toString();
  }

  public JSONObject listProjectsDataTable(HttpSession session, JSONObject json) {
    try {
      final JSONObject j = new JSONObject();
      final JSONArray jsonArray = new JSONArray();
      for (Project project : requestManager.listAllProjects()) {
        jsonArray.add(JsonSanitizer.sanitize("[\"" + project.getName() + "\",\"" + project.getAlias() + "\",\"" + project.getDescription()
            + "\",\"" + project.getProgress().getKey() + "\",\"" + project.getProjectId() + "\",'" + "<a href=\"/miso/project/"
            + project.getId() + "\"><span class=\"ui-icon ui-icon-pencil\"></span></a>" + "']"));

      }
      j.put("projectsArray", jsonArray);
      return j;
    } catch (final IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  private String colorOverviewState(Boolean bool) {
    if (bool) {
      return " partitionOccupied";
    } else {
      return "";
    }
  }

  private String checkSamples(Long projectId) throws IOException {
    final Collection<Sample> samples = requestManager.listAllSamplesByProjectId(projectId);
    if (samples.size() > 0) {
      int pass = 0;
      for (final Sample s : samples) {
        if (s.getQcPassed() != null) {
          if (s.getQcPassed()) {
            pass++;
          }
        }
      }
      if (pass == samples.size()) {
        return "green";
      } else {
        return "yellow";
      }
    } else {
      return "gray";
    }
  }

  private String checkLibraries(Long projectId) throws IOException {
    int pass = 0;
    final Collection<Library> libs = requestManager.listAllLibrariesByProjectId(projectId);
    if (libs.size() > 0) {
      for (final Library l : libs) {
        if (l.getLibraryQCs().size() > 0) {
          pass += l.getLibraryQCs().size();
        }
      }
      if (pass >= libs.size()) {
        return "green";
      } else {
        return "yellow";
      }
    } else {
      return "gray";
    }
  }

  private String checkRuns(Long projectId) throws IOException {
    int pass = 0;
    final Collection<Run> runs = requestManager.listAllRunsByProjectId(projectId);
    if (runs.size() > 0) {
      for (final Run run : runs) {
        if (run.getStatus() != null && run.getStatus().getHealth() != null && run.getStatus().getHealth().getKey().equals("Completed")) {
          pass++;
        }
      }

      if (pass == runs.size()) {
        return "green";
      } else {
        return "yellow";
      }
    } else {
      return "gray";
    }
  }

  public JSONObject editProjectTrafficLight(HttpSession session, JSONObject json) {
    final Long projectId = json.getLong("projectId");
    final StringBuffer b = new StringBuffer();
    try {
      final String trafSample = checkSamples(projectId);
      final String trafLib = checkLibraries(projectId);
      final String trafRun = checkRuns(projectId);

      b.append("<div id=\"projectstatustrafficlight\">\n" + "        <table class=\"traf\">\n"
          + "<thead><tr><th width=\"33%\">Samples</th><th width=\"33%\">Libraries</th><th width=\"33%\">Runs</th></tr></thead>"
          + "            <tbody>\n" + "            <tr>\n" + "                <td width=\"33%\"><img src=\"/styles/images/status/"
          + trafSample + ".png\"/></td>\n" + "                <td width=\"33%\"><img src=\"/styles/images/status/" + trafLib
          + ".png\"/></td>\n" + "                <td width=\"33%\"><img src=\"/styles/images/status/" + trafRun + ".png\"/></td>\n"
          + "            </tr>\n" + "            </tbody>\n" + "        </table>\n" + "</div>");

      return JSONUtils.JSONObjectResponse("html", b.toString());
    } catch (final IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public JSONObject printAllSampleBarcodes(HttpSession session, JSONObject json) {
    final Long projectId = json.getLong("projectId");
    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());

      String serviceName = null;
      if (json.has("serviceName")) {
        serviceName = json.getString("serviceName");
      }

      MisoPrintService<File, Barcodable, PrintContext<File>> mps = null;
      if (serviceName == null) {
        final Collection<MisoPrintService> services = printManager.listPrintServicesByBarcodeableClass(Sample.class);
        if (services.size() == 1) {
          mps = services.iterator().next();
        } else {
          return JSONUtils
              .SimpleJSONError("No serviceName specified, but more than one available service able to print this barcode type.");
        }
      } else {
        mps = printManager.getPrintService(serviceName);
      }

      final Queue<File> thingsToPrint = new LinkedList<File>();
      final Collection<Sample> samples = requestManager.listAllSamplesByProjectId(projectId);
      for (final Sample sample : samples) {
        // autosave the barcode if none has been previously generated
        if (isStringEmptyOrNull(sample.getIdentificationBarcode())) {
          requestManager.saveSample(sample);
        }
        final File f = mps.getLabelFor(sample);
        if (f != null) thingsToPrint.add(f);
      }
      final PrintJob pj = printManager.print(thingsToPrint, mps.getName(), user);
      return JSONUtils.SimpleJSONResponse("Job " + pj.getJobId() + " : Barcodes printed.");
    } catch (final MisoPrintException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    } catch (final IOException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    }
  }

  public JSONObject printSelectedSampleBarcodes(HttpSession session, JSONObject json) {
    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      final JSONArray ss = JSONArray.fromObject(json.getString("samples"));

      String serviceName = null;
      if (json.has("serviceName")) {
        serviceName = json.getString("serviceName");
      }

      MisoPrintService<File, Barcodable, PrintContext<File>> mps = null;
      if (serviceName == null) {
        final Collection<MisoPrintService> services = printManager.listPrintServicesByBarcodeableClass(Sample.class);
        if (services.size() == 1) {
          mps = services.iterator().next();
        } else {
          return JSONUtils
              .SimpleJSONError("No serviceName specified, but more than one available service able to print this barcode type.");
        }
      } else {
        mps = printManager.getPrintService(serviceName);
      }

      final Queue<File> thingsToPrint = new LinkedList<File>();
      for (final JSONObject p : (Iterable<JSONObject>) ss) {
        final Long sampleId = p.getLong("sampleId");
        final Sample sample = requestManager.getSampleById(sampleId);
        if (isStringEmptyOrNull(sample.getIdentificationBarcode())) {
          requestManager.saveSample(sample);
        }
        final File f = mps.getLabelFor(sample);
        if (f != null) thingsToPrint.add(f);
      }
      final PrintJob pj = printManager.print(thingsToPrint, mps.getName(), user);
      return JSONUtils.SimpleJSONResponse("Job " + pj.getJobId() + " : Barcodes printed.");
    } catch (final MisoPrintException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    } catch (final IOException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    }
  }

  public JSONObject printAllLibraryBarcodes(HttpSession session, JSONObject json) {
    final Long projectId = json.getLong("projectId");
    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());

      String serviceName = null;
      if (json.has("serviceName")) {
        serviceName = json.getString("serviceName");
      }

      MisoPrintService<File, Barcodable, PrintContext<File>> mps = null;
      if (serviceName == null) {
        final Collection<MisoPrintService> services = printManager.listPrintServicesByBarcodeableClass(Library.class);
        if (services.size() == 1) {
          mps = services.iterator().next();
        } else {
          return JSONUtils
              .SimpleJSONError("No serviceName specified, but more than one available service able to print this barcode type.");
        }
      } else {
        mps = printManager.getPrintService(serviceName);
      }

      final Queue<File> thingsToPrint = new LinkedList<File>();
      final Collection<Library> libraries = requestManager.listAllLibrariesByProjectId(projectId);
      for (final Library library : libraries) {
        // autosave the barcode if none has been previously generated
        if (isStringEmptyOrNull(library.getIdentificationBarcode())) {
          requestManager.saveLibrary(library);
        }
        final File f = mps.getLabelFor(library);
        if (f != null) thingsToPrint.add(f);
      }
      final PrintJob pj = printManager.print(thingsToPrint, mps.getName(), user);
      return JSONUtils.SimpleJSONResponse("Job " + pj.getJobId() + " : Barcodes printed.");
    } catch (final MisoPrintException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    } catch (final IOException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    }
  }

  public JSONObject printSelectedLibraryBarcodes(HttpSession session, JSONObject json) {
    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      final JSONArray ss = JSONArray.fromObject(json.getString("libraries"));

      String serviceName = null;
      if (json.has("serviceName")) {
        serviceName = json.getString("serviceName");
      }

      MisoPrintService<File, Barcodable, PrintContext<File>> mps = null;
      if (serviceName == null) {
        final Collection<MisoPrintService> services = printManager.listPrintServicesByBarcodeableClass(Library.class);
        if (services.size() == 1) {
          mps = services.iterator().next();
        } else {
          return JSONUtils
              .SimpleJSONError("No serviceName specified, but more than one available service able to print this barcode type.");
        }
      } else {
        mps = printManager.getPrintService(serviceName);
      }

      final Queue<File> thingsToPrint = new LinkedList<File>();
      for (final JSONObject p : (Iterable<JSONObject>) ss) {
        final Long libraryId = p.getLong("libraryId");
        final Library library = requestManager.getLibraryById(libraryId);
        // autosave the barcode if none has been previously generated
        if (isStringEmptyOrNull(library.getIdentificationBarcode())) {
          requestManager.saveLibrary(library);
        }
        final File f = mps.getLabelFor(library);
        if (f != null) thingsToPrint.add(f);
      }
      final PrintJob pj = printManager.print(thingsToPrint, mps.getName(), user);
      return JSONUtils.SimpleJSONResponse("Job " + pj.getJobId() + " : Barcodes printed.");
    } catch (final MisoPrintException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    } catch (final IOException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    }
  }

  public JSONObject printAllLibraryDilutionBarcodes(HttpSession session, JSONObject json) {
    final Long projectId = json.getLong("projectId");
    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());

      String serviceName = null;
      if (json.has("serviceName")) {
        serviceName = json.getString("serviceName");
      }

      MisoPrintService<File, Barcodable, PrintContext<File>> mps = null;
      if (serviceName == null) {
        final Collection<MisoPrintService> services = printManager.listPrintServicesByBarcodeableClass(Dilution.class);
        if (services.size() == 1) {
          mps = services.iterator().next();
        } else {
          return JSONUtils
              .SimpleJSONError("No serviceName specified, but more than one available service able to print this barcode type.");
        }
      } else {
        mps = printManager.getPrintService(serviceName);
      }

      final Queue<File> thingsToPrint = new LinkedList<File>();
      final Collection<LibraryDilution> libraryDilutions = requestManager.listAllLibraryDilutionsByProjectId(projectId);
      for (final LibraryDilution libraryDilution : libraryDilutions) {
        // autosave the barcode if none has been previously generated
        if (isStringEmptyOrNull(libraryDilution.getIdentificationBarcode())) {
          requestManager.saveLibraryDilution(libraryDilution);
        }
        final File f = mps.getLabelFor(libraryDilution);
        if (f != null) thingsToPrint.add(f);
      }
      final PrintJob pj = printManager.print(thingsToPrint, mps.getName(), user);
      return JSONUtils.SimpleJSONResponse("Job " + pj.getJobId() + " : Barcodes printed.");
    } catch (final MisoPrintException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    } catch (final IOException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    }
  }

  public JSONObject printSelectedLibraryDilutionBarcodes(HttpSession session, JSONObject json) {
    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      final JSONArray ss = JSONArray.fromObject(json.getString("dilutions"));

      String serviceName = null;
      if (json.has("serviceName")) {
        serviceName = json.getString("serviceName");
      }

      MisoPrintService<File, Barcodable, PrintContext<File>> mps = null;
      if (serviceName == null) {
        final Collection<MisoPrintService> services = printManager.listPrintServicesByBarcodeableClass(Dilution.class);
        if (services.size() == 1) {
          mps = services.iterator().next();
        } else {
          return JSONUtils
              .SimpleJSONError("No serviceName specified, but more than one available service able to print this barcode type.");
        }
      } else {
        mps = printManager.getPrintService(serviceName);
      }

      final Queue<File> thingsToPrint = new LinkedList<File>();
      for (final JSONObject p : (Iterable<JSONObject>) ss) {
        final Long dilutionId = p.getLong("dilutionId");
        final LibraryDilution libraryDilution = requestManager.getLibraryDilutionById(dilutionId);
        // autosave the barcode if none has been previously generated
        if (isStringEmptyOrNull(libraryDilution.getIdentificationBarcode())) {
          requestManager.saveLibraryDilution(libraryDilution);
        }
        final File f = mps.getLabelFor(libraryDilution);
        if (f != null) thingsToPrint.add(f);
      }
      final PrintJob pj = printManager.print(thingsToPrint, mps.getName(), user);
      return JSONUtils.SimpleJSONResponse("Job " + pj.getJobId() + " : Barcodes printed.");
    } catch (final MisoPrintException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    } catch (final IOException e) {
      log.error("print barcodes", e);
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    }
  }

  public JSONObject generateSampleDeliveryForm(HttpSession session, JSONObject json) {
    Boolean plate = false;
    if ("yes".equals(json.getString("plate"))) {
      plate = true;
    }
    final Long projectId = json.getLong("projectId");
    final List<Sample> samples = new ArrayList<Sample>();
    if (json.has("samples")) {
      try {
        final JSONArray a = JSONArray.fromObject(json.get("samples"));
        for (final JSONObject j : (Iterable<JSONObject>) a) {
          samples.add(requestManager.getSampleById(j.getLong("sampleId")));
        }
        final File f = misoFileManager.getNewFile(Project.class, projectId.toString(),
            "SampleInformationForm-" + LimsUtils.getCurrentDateAsString() + ".odt");

        FormUtils.createSampleDeliveryForm(samples, f, plate);
        return JSONUtils.SimpleJSONResponse("" + f.getName().hashCode());
      } catch (final Exception e) {
        log.error("generate sample deliver form", e);
        return JSONUtils.SimpleJSONError("Failed to create sample delivery form: " + e.getMessage());
      }
    } else {
      return JSONUtils.SimpleJSONError("No samples selected to generate delivery form");
    }
  }

  public JSONObject downloadBulkSampleInputForm(HttpSession session, JSONObject json) {
    if (json.has("projectId") && json.has("documentFormat")) {
      final Long projectId = json.getLong("projectId");
      final String documentFormat = json.getString("documentFormat");
      try {
        final File f = misoFileManager.getNewFile(Project.class, projectId.toString(),
            "BulkInputForm-" + LimsUtils.getCurrentDateAsString() + "." + documentFormat);
        FormUtils.createSampleInputSpreadsheet(requestManager.getProjectById(projectId).getSamples(), f);
        return JSONUtils.SimpleJSONResponse("" + f.getName().hashCode());
      } catch (final Exception e) {
        e.printStackTrace();
        return JSONUtils.SimpleJSONError("Failed to get bulk input form: " + e.getMessage());
      }
    } else {
      return JSONUtils.SimpleJSONError("Missing project ID or document format supplied.");
    }
  }

  public JSONObject visualiseBulkSampleInputForm(HttpSession session, JSONObject json) {
    final JSONObject samplelist = (JSONObject) session.getAttribute("bulksamples");
    if (samplelist == null) {
      final JSONObject error = (JSONObject) session.getAttribute("bulkerror");
      if (error != null) {
        return JSONUtils.SimpleJSONError("Failed to get bulk input sheet from session: " + error.getString("bulkerror"));
      }
      return JSONUtils.SimpleJSONError("Failed to get bulk input sheet from session.");
    }

    final JSONArray samples = samplelist.getJSONArray("bulksamples");
    if (samples != null) {
      final StringBuilder sb = new StringBuilder();
      sb.append("<div style='width: 100%;'>");
      final ObjectMapper mapper = new ObjectMapper();
      for (final JSONObject sam : (Iterable<JSONObject>) samples) {
        Sample s = null;
        try {
          s = mapper.readValue(sam.toString(), SampleImpl.class);
          sb.append("<a class=\"dashboardresult\" href=\"/miso/sample/" + s.getId()
              + "\"><div onMouseOver=\"this.className=&#39dashboardhighlight&#39\" onMouseOut=\"this.className=&#39dashboard&#39\" class=\"dashboard\">");
          sb.append("Name: <b>" + s.getName() + "</b><br/>");
          sb.append("Alias: <b>" + s.getAlias() + "</b><br/>");

          final Set<Pool> pools = new HashSet<Pool>();

          for (final Library l : s.getLibraries()) {
            if (!l.getLibraryDilutions().isEmpty()) {
              for (final Dilution ld : l.getLibraryDilutions()) {
                if (!ld.getPools().isEmpty()) {
                  pools.addAll(ld.getPools());
                }
              }
            }
          }

          if (!pools.isEmpty()) {
            sb.append("Pools: <ul>");
            for (final Pool p : pools) {
              sb.append("<li>").append(p.getName()).append(" (").append(p.getAlias()).append(") - ").append(p.getDilutions().size())
                  .append(" dilutions<li>");
            }
            sb.append("</ul><br/>");
          }

          sb.append("</div></a>");
        } catch (final IOException e) {
          log.error("visualise bulk sample input form", e);
        } finally {
          sb.append("</div>");
          session.removeAttribute("bulksamples");
        }
      }

      return JSONUtils.SimpleJSONResponse(sb.toString());
    } else {
      return JSONUtils.SimpleJSONError("Failed to get bulk input sheet from session.");
    }
  }

  public JSONObject downloadPlateInputForm(HttpSession session, JSONObject json) {
    if (json.has("projectId") && json.has("documentFormat")) {
      final Long projectId = json.getLong("projectId");
      final String documentFormat = json.getString("documentFormat");
      try {
        final File f = misoFileManager.getNewFile(Project.class, projectId.toString(),
            "PlateInputForm-" + LimsUtils.getCurrentDateAsString() + "." + documentFormat);
        // TODO select a single sample to base sheet on?
        FormUtils.createPlateInputSpreadsheet(f);
        return JSONUtils.SimpleJSONResponse("" + f.getName().hashCode());
      } catch (final Exception e) {
        log.error("download plate input", e);
        return JSONUtils.SimpleJSONError("Failed to get plate input form: " + e.getMessage());
      }
    } else {
      return JSONUtils.SimpleJSONError("Missing project ID or document format supplied.");
    }
  }

  public JSONObject watchOverview(HttpSession session, JSONObject json) {
    final Long overviewId = json.getLong("overviewId");
    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      final ProjectOverview overview = requestManager.getProjectOverviewById(overviewId);
      if (!overview.getWatchers().contains(user)) {
        watchManager.watch(overview, user);
        watchManager.watch(overview.getProject(), user);
        requestManager.saveProjectOverview(overview);
        requestManager.saveProject(overview.getProject());
      }
      return JSONUtils.SimpleJSONResponse("OK");
    } catch (final IOException e) {
      log.error("watch overview", e);
    }
    return JSONUtils.SimpleJSONError("Unable to watch/unwatch overview");
  }

  public JSONObject unwatchOverview(HttpSession session, JSONObject json) {
    final Long overviewId = json.getLong("overviewId");
    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      final ProjectOverview overview = requestManager.getProjectOverviewById(overviewId);
      if (!overview.getProject().getSecurityProfile().getOwner().equals(user)) {
        if (overview.getWatchers().contains(user)) {
          watchManager.unwatch(overview, user);
          watchManager.unwatch(overview.getProject(), user);
          requestManager.saveProjectOverview(overview);
          requestManager.saveProject(overview.getProject());
        }
        return JSONUtils.SimpleJSONResponse("OK");
      } else {
        return JSONUtils.SimpleJSONError("Cannot unwatch an entity of which you are the owner.");
      }
    } catch (final IOException e) {
      log.error("unwatch overview", e);
    }
    return JSONUtils.SimpleJSONError("Unable to watch/unwatch overview");
  }

  public JSONObject listWatchOverview(HttpSession session, JSONObject json) {
    final Long overviewId = json.getLong("overviewId");
    final StringBuilder sb = new StringBuilder();
    final JSONObject j = new JSONObject();
    try {
      final User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      final ProjectOverview overview = requestManager.getProjectOverviewById(overviewId);
      sb.append("<ul class='bullets' style='margin-left: -30px;'>");
      for (final User theUser : overview.getWatchers()) {
        sb.append("<li>");
        sb.append(theUser.getFullName());
        sb.append("</li>");
      }
      sb.append("</ul>");
      j.put("watchers", sb.toString());
      return j;
    } catch (final IOException e) {
      log.error("list watch overview", e);
    }
    return JSONUtils.SimpleJSONError("Unable to list watchers");
  }

  public JSONObject listSamplesByProject(HttpSession session, JSONObject json) {
    final Long projectId = json.getLong("projectId");

    try {
      final JSONObject j = new JSONObject();
      final JSONArray jsonArray = new JSONArray();
      for (final Sample sample : requestManager.listAllSamplesByProjectId(projectId)) {
        jsonArray.add("{'id':'" + sample.getId() + "'," + "'name':'" + sample.getName() + "'," + "'alias':'" + sample.getAlias() + "',"
            + "'type':'" + sample.getSampleType() + "'," + "'description':'" + sample.getDescription() + "'}");
      }
      j.put("array", jsonArray);
      return j;
    } catch (final IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public JSONObject addSampleGroup(HttpSession session, JSONObject json) {
    final Long overviewId = json.getLong("overviewId");
    try {
      final ProjectOverview overview = requestManager.getProjectOverviewById(overviewId);

      final Set<Sample> samples = new HashSet<>();
      if (json.has("samples")) {
        final JSONArray a = JSONArray.fromObject(json.get("samples"));
        for (final JSONObject j : (Iterable<JSONObject>) a) {
          if (j.has("sampleId")) {
            samples.add(requestManager.getSampleById(j.getLong("sampleId")));
          } else {
            return JSONUtils.SimpleJSONError("Unable to add Sample Group: invalid sample set JSON has missing sampleId");
          }
        }
      }

      final EntityGroup<ProjectOverview, Sample> osg = new EntityGroupImpl<>();
      osg.setEntities(samples);
      osg.setParent(overview);
      overview.setSampleGroup(osg);

      requestManager.saveProjectOverview(overview);
      requestManager.saveProject(overview.getProject());

      return JSONUtils.SimpleJSONResponse("OK");
    } catch (final IOException e) {
      log.error("add sample group", e);
      return JSONUtils.SimpleJSONError("Unable to add Sample Group: " + e.getMessage());
    }
  }

  public JSONObject addSamplesToGroup(HttpSession session, JSONObject json) {
    final Long overviewId = json.getLong("overviewId");
    final Long groupId = json.getLong("groupId");
    try {
      final ProjectOverview overview = requestManager.getProjectOverviewById(overviewId);
      final EntityGroup<ProjectOverview, Sample> osg = overview.getSampleGroup();

      if (osg != null && groupId != null && osg.getId() == groupId.longValue()) {
        if (json.has("samples")) {
          final JSONArray a = JSONArray.fromObject(json.get("samples"));
          for (final JSONObject j : (Iterable<JSONObject>) a) {
            if (j.has("sampleId")) {
              final Sample s = requestManager.getSampleById(j.getLong("sampleId"));
              if (osg.getEntities().contains(s)) {
                log.error("Sample group already contains " + s.getName());
              } else {
                osg.addEntity(s);
              }
            } else {
              return JSONUtils.SimpleJSONError("Unable to add Sample Group: invalid sample set JSON has missing sampleId");
            }
          }
        }
      }

      requestManager.saveProjectOverview(overview);
      requestManager.saveProject(overview.getProject());

      return JSONUtils.SimpleJSONResponse("OK");
    } catch (final IOException e) {
      log.error("add samples to group", e);
      return JSONUtils.SimpleJSONError("Unable to add Sample Group: " + e.getMessage());
    }
  }
}
