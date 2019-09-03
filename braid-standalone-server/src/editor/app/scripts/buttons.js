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

import {retrieveAndUpdateServices} from 'scripts/workers';

import Helpers from 'scripts/helpers'

const helpers = new Helpers();

export default class Buttons {
  constructor() {
    this.onCreateService = this.onCreateService.bind(this);
  }

  onCreateService() {
    const serviceName = $('#newService').val();
    const tooltip = document.querySelector('.tooltip');
    const isValidServiceName = helpers.checkCreatedService(serviceName);
    if(isValidServiceName) {
      helpers.getServiceScript(serviceName, function () {
        retrieveAndUpdateServices(serviceName);
        $('#newService').val("");
        tooltip.classList.remove('shown');
      });
    } else {
      helpers.formatTooltip(serviceName)
    }
  }

  onSave(e) {
    const script = editor.getValue();
    const service = helpers.getSelectedService();

    $.post("/api/services/" + service + "/script", script)
      .done(function () {
        console.log("saved")
        $('#saveBtn').prop('disabled', true)
      })
      .fail(function (e) {
        console.log("failed to save", e);
      })
  }

  onFormat(e) {
    editor.trigger('', 'editor.action.formatDocument');
  }

  onDelete(e) {
    const serviceName = helpers.getSelectedService();
    const callValue = "/api/services/" + serviceName;
    $.ajax({
      url: callValue, method: 'DELETE', success: () => {
        helpers.setSelectedService('');
        retrieveAndUpdateServices('');
      }
    });
  }
}