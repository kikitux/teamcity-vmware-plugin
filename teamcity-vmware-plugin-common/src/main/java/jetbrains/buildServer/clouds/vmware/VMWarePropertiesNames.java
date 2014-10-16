/*
 *
 *  * Copyright 2000-2014 JetBrains s.r.o.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package jetbrains.buildServer.clouds.vmware;

/**
 * @author Sergey.Pak
 *         Date: 4/23/2014
 *         Time: 6:42 PM
 */
public interface VMWarePropertiesNames {
  public static final String AGENT_NAME = "guestinfo.tcAgentName";
  public static final String AUTH_TOKEN = "guestinfo.tcAuthToken";
  public static final String SERVER_URL = "guestinfo.tcServerUrl";

  public static final String INSTANCE_NAME = "guestinfo.instanceName";
  public static final String IMAGE_NAME = "guestinfo.imageName";
  public static final String USER_DATA = "guestinfo.userData";
}
