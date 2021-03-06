package jetbrains.buildServer.clouds.vmware;

import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Task;
import java.io.File;
import java.rmi.RemoteException;
import java.util.*;

import com.intellij.util.WaitFor;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.clouds.base.connector.CloudAsyncTaskExecutor;
import jetbrains.buildServer.clouds.base.tasks.UpdateInstancesTask;
import jetbrains.buildServer.clouds.base.types.CloneBehaviour;
import jetbrains.buildServer.clouds.server.impl.profile.CloudClientParametersImpl;
import jetbrains.buildServer.clouds.server.impl.profile.CloudImageParametersImpl;
import jetbrains.buildServer.clouds.vmware.connector.VMWareApiConnector;
import jetbrains.buildServer.clouds.vmware.stubs.FakeApiConnector;
import jetbrains.buildServer.clouds.vmware.stubs.FakeModel;
import jetbrains.buildServer.clouds.vmware.stubs.FakeVirtualMachine;
import jetbrains.buildServer.clouds.vmware.tasks.VmwareUpdateTaskManager;
import jetbrains.buildServer.util.FileUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Sergey.Pak
 *         Date: 5/20/2014
 *         Time: 3:16 PM
 */
@Test
public class VmwareCloudImageTest extends BaseTestCase {

  private CloudAsyncTaskExecutor myTaskExecutor;
  private VMWareApiConnector myApiConnector;
  private VmwareCloudImage myImage;
  private VmwareCloudImageDetails myImageDetails;
  private File myIdxStorage;
  private UpdateInstancesTask<VmwareCloudInstance, VmwareCloudImage, VMWareCloudClient> myUpdateTask;
  private VMWareCloudClient myCloudClient;
  private CloudProfile myProfile;


  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    FakeModel.instance().clear();
    myTaskExecutor = new CloudAsyncTaskExecutor("Test-vmware");
    myApiConnector = new FakeApiConnector(VmwareCloudIntegrationTest.TEST_SERVER_UUID, VmwareCloudIntegrationTest.PROFILE_ID);
    myIdxStorage = createTempDir();
    Map<String, String> params = new HashMap<>();
    params.put("nickname", "imageNickname");
    params.put("sourceVmName", "srcVM");
    params.put("snapshot", "srcVMSnap");
    params.put("folder", "folderId");
    params.put("pool", "rpId");
    params.put("behaviour", CloneBehaviour.FRESH_CLONE.toString());
    params.put("maxInstances", "5");
    CloudImageParameters imageParameters = new CloudImageParametersImpl(params);

    myImageDetails = new VmwareCloudImageDetails(imageParameters);

    FakeModel.instance().addDatacenter("dc2");
    FakeModel.instance().addFolder("folderId").setParent("dc2", Datacenter.class);
    FakeModel.instance().addVM("srcVM").setParentFolder("folderId");
    FakeModel.instance().addResourcePool("rpId").setParentFolder("folderId");

    FakeModel.instance().addVMSnapshot("srcVM", "srcVMSnap");

    myProfile = VmwareTestUtils.createProfileFromProps(new CloudClientParametersImpl(Collections.emptyMap(), Collections.emptyList()));
    myImage = new VmwareCloudImage(myApiConnector, myImageDetails, myTaskExecutor, myIdxStorage, myProfile);

    myCloudClient = new VMWareCloudClient(myProfile, myApiConnector, new VmwareUpdateTaskManager(), createTempDir());
    myCloudClient.populateImagesData(Collections.singletonList(myImageDetails));
    myUpdateTask = new UpdateInstancesTask<VmwareCloudInstance, VmwareCloudImage, VMWareCloudClient>(myApiConnector, myCloudClient, 10*1000, false);
  }

  public void check_clone_name_generation(){
    for (int i=0; i<10; i++){
      assertEquals(String.format("%s-%d", myImageDetails.getSourceId(), i + 1), myImage.generateNewVmName());
    }
    FileUtil.delete(myIdxStorage);
    final String newName = myImage.generateNewVmName();
    assertTrue(newName.startsWith(myImage.getName()));
    final int i = Integer.parseInt(newName.substring(myImage.getName().length() + 1));
    assertTrue(i > 100000);
  }

  public void check_can_start_new_instance_limits() throws RemoteException, InterruptedException {
    final CloudInstanceUserData data = new CloudInstanceUserData("aaa", "bbbb", "localhost", 10000l, "profileDescr", Collections.<String, String>emptyMap());
    assertTrue(myImage.canStartNewInstance());
    myImage.startNewInstance(data);
    assertTrue(myImage.canStartNewInstance());
    myImage.startNewInstance(data);
    assertTrue(myImage.canStartNewInstance());
    myImage.startNewInstance(data);
    assertTrue(myImage.canStartNewInstance());
    myImage.startNewInstance(data);
    assertTrue(myImage.canStartNewInstance());
    final VmwareCloudInstance instance2Stop = myImage.startNewInstance(data);
    assertFalse(myImage.canStartNewInstance());
    new WaitFor(5*1000){

      @Override
      protected boolean condition() {
        return instance2Stop.getStatus() == InstanceStatus.RUNNING;
      }
    };
    final FakeVirtualMachine vm2Stop = FakeModel.instance().getVirtualMachine(instance2Stop.getName());
    final String result = vm2Stop.powerOffVM_Task().waitForTask();
    assertEquals(Task.SUCCESS, result);
    instance2Stop.setStatus(InstanceStatus.STOPPED);
    assertTrue(myImage.canStartNewInstance());
    System.setProperty(VmwareConstants.CONSIDER_STOPPED_VMS_LIMIT, "true");
    assertFalse(myImage.canStartNewInstance());
    System.getProperties().remove(VmwareConstants.CONSIDER_STOPPED_VMS_LIMIT);
    assertTrue(myImage.canStartNewInstance());
  }

  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
  }

}
