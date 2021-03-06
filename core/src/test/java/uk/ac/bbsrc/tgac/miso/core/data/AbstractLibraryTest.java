package uk.ac.bbsrc.tgac.miso.core.data;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.eaglegenomics.simlims.core.SecurityProfile;
import com.eaglegenomics.simlims.core.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.SampleImpl;
import uk.ac.bbsrc.tgac.miso.core.security.SecurableByProfile;

public class AbstractLibraryTest {

  AbstractLibrary al;

  protected ObjectMapper mapper;

  @Before
  public void setUp() throws Exception {
    al = new AbstractLibrary() {

      @Override
      public ChangeLog createChangeLog(String summary, String columnsChanged, User user) {
        return null;
      }
    };
    mapper = new ObjectMapper();
  }

  @Test
  public final void testInheritPermissions() {
    final SecurableByProfile parent = Mockito.mock(SecurableByProfile.class);
    final SecurityProfile mockSecurityProfile = Mockito.mock(SecurityProfile.class);
    final User mockUser = Mockito.mock(User.class);
    when(parent.getSecurityProfile()).thenReturn(mockSecurityProfile);
    when(mockSecurityProfile.getOwner()).thenReturn(mockUser);

    assertNull(al.getSecurityProfile());
    al.inheritPermissions(parent);
    assertNotNull(al.getSecurityProfile());
  }

  @Test(expected = SecurityException.class)
  public void testInheritPermissionsException() {
    final SecurableByProfile parent = Mockito.mock(SecurableByProfile.class);
    final SecurityProfile mockSecurityProfile = Mockito.mock(SecurityProfile.class);
    when(parent.getSecurityProfile()).thenReturn(mockSecurityProfile);
    al.inheritPermissions(parent);
  }

  @Test
  public final void testLibrarySerialization() throws Exception {
    Sample sample = new SampleImpl();
    Library library = new LibraryImpl();
    library.setSample(sample);
    library.setAlias("TestLib");
    String mappedLib = mapper.writer().writeValueAsString(library);
    assertThat(mappedLib, containsString("TestLib"));
  }
}
