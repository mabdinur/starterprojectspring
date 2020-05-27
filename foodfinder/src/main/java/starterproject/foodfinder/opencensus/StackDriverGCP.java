package starterproject.foodfinder.opencensus;

/**
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import java.io.IOException;

public class StackDriverGCP {

  public static void createAndRegisterGoogleCloudPlatform() throws IOException {
	  StackdriverTraceExporter.createAndRegister(
		      StackdriverTraceConfiguration.builder()
		        .setProjectId("lorax-oss-2020")
		        .build());
  }
}