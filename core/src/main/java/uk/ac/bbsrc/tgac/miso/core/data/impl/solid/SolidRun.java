/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey @ TGAC
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

package uk.ac.bbsrc.tgac.miso.core.data.impl.solid;

import static uk.ac.bbsrc.tgac.miso.core.util.LimsUtils.isStringEmptyOrNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.eaglegenomics.simlims.core.SecurityProfile;
import com.eaglegenomics.simlims.core.User;

import uk.ac.bbsrc.tgac.miso.core.data.impl.RunImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.StatusImpl;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.util.SubmissionUtils;
import uk.ac.bbsrc.tgac.miso.core.util.UnicodeReader;

/**
 * uk.ac.bbsrc.tgac.miso.core.data.impl.solid
 * <p/>
 * 
 * @author Rob Davey
 * @since 0.0.2
 */
public class SolidRun extends RunImpl {

  private static final long serialVersionUID = 1L;
  private static final Logger log = LoggerFactory.getLogger(SolidRun.class);

  public SolidRun() {
    setPlatformType(PlatformType.SOLID);
    setStatus(new StatusImpl());
    setSecurityProfile(new SecurityProfile());
  }

  public SolidRun(User user) {
    setPlatformType(PlatformType.SOLID);
    setStatus(new StatusImpl());
    setSecurityProfile(new SecurityProfile(user));
  }

  public SolidRun(String statusXml) {
    this(statusXml, null);
  }

  public SolidRun(String statusXml, User user) {
    try {
      if (!isStringEmptyOrNull(statusXml)) {
        String runDirRegex = "([A-z0-9\\-]+)_([0-9]{8})_(.*)";
        Pattern runRegex = Pattern.compile(runDirRegex);
        Document statusDoc = SubmissionUtils.emptyDocument();
        SubmissionUtils.transform(new UnicodeReader(statusXml), statusDoc);

        String runName;
        if (statusDoc.getDocumentElement().getTagName().equals("error")) {
          runName = (statusDoc.getElementsByTagName("RunName").item(0).getTextContent());
        } else {
          runName = (statusDoc.getElementsByTagName("name").item(0).getTextContent());
          if (statusDoc.getElementsByTagName("name").getLength() != 0) {
            for (int i = 0; i < statusDoc.getElementsByTagName("name").getLength(); i++) {
              Element e = (Element) statusDoc.getElementsByTagName("name").item(i);
              Matcher m = runRegex.matcher(e.getTextContent());
              if (m.matches()) {
                runName = e.getTextContent();
              }
            }
          }
          setPlatformRunId(Integer.parseInt(statusDoc.getElementsByTagName("id").item(0).getTextContent()));
        }
        setAlias(runName);
        setFilePath(runName);
        setPairedEnd(false);

        Matcher m = runRegex.matcher(runName);
        if (m.matches()) {
          setDescription(m.group(3));
          if (m.group(3).startsWith("MP") || m.group(3).startsWith("PE")) {
            setPairedEnd(true);
          }
        } else {
          setDescription(runName);
        }

        setPlatformType(PlatformType.SOLID);
        setStatus(new SolidStatus(statusXml));
        if (user != null) {
          setSecurityProfile(new SecurityProfile(user));
        } else {
          setSecurityProfile(new SecurityProfile());
        }
      } else {
        log.error("No status XML for this run");
      }
    } catch (ParserConfigurationException e) {
      log.error("parse status XML", e);
    } catch (TransformerException e) {
      log.error("parse status XML", e);
    }
  }

  /**
   * Method buildReport ...
   */
  @Override
  public void buildReport() {

  }
}
