/*
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Helpers from 'scripts/helpers';

const helpers = new Helpers();

export function ensureServiceIsCreated(callback) {
  const params = helpers.parseURL(document.URL).searchObject;
  if (typeof(params['service']) !== 'undefined' && params.service !== null) {
    helpers.getServiceScript(params.service, function() {
      callback(params.service);
    })
  } else {
    callback(null);
  }
}

export function retrieveAndUpdateServices(selectedService) {
  $.get("/api/services", function (data) {
    updateServices(data, selectedService);
  });
}

export function updateServices(services, selectedService) {
  const selection = $('#services');
  populateServiceOptions(selection, services);
  if (selectedService) {
    selection.val(selectedService).trigger('change');
    switchService(selectedService);
  }
}

export function switchService(selectedService){
  helpers.setSelectedService(selectedService);
  helpers.getServiceScript(selectedService, function(script) {
    helpers.setEditorContents(script)
    $('#saveBtn').prop('disabled', true)
  });
  helpers.getJavaHeaders(selectedService, helpers.showExistingServices);
  helpers.selectHighlight(helpers.getSelectedService());
  helpers.getEndPoint(selectedService);
  editor.focus();
}

function populateServiceOptions(selection, services) {
  selection.empty();

  for (var idx in services) {
    const service = services[idx];
    const option = $('<li>', {value: service, text: service})
    //option.click(onServiceSelect);
    selection.append(option);
  }
}