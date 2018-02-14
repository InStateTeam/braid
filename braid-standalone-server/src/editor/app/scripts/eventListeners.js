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

import Buttons from 'scripts/buttons';
import {switchService} from 'scripts/workers';
import Helpers from 'scripts/helpers';

export default class EventListeners {
  constructor(){
    this.Buttons = new Buttons();
    this.Helpers = new Helpers();
    this.init();
  }

  init(){
    this.functionButtonsClickEvents();
    this.textBoxKeyEvents(); 
    this.modalClickEvents();    
    this.tooltipEvents();
    this.copyServiceClickEvent();
    this.serviceSelectEvent();
  }

  functionButtonsClickEvents(){
    let functionButtons = document.querySelector('.functions');
    let self = this;
    functionButtons.addEventListener('click', (e) => {

      if(e.target.id === 'createServiceBtn'){
        self.Buttons.onCreateService();
      }
      if(e.target.id === 'saveBtn'){
       self.Buttons.onSave(e); 
      }
      if(e.target.id === 'formatBtn'){
       self.Buttons.onFormat(e); 
      }
      if(e.target.id === 'deleteBtn'){
       self.Buttons.onDelete(e); 
      }
    })
  }

  textBoxKeyEvents(){
    let textBoxCreate = document.querySelector('#newService');
    let self = this;
    textBoxCreate.addEventListener('keyup', (e) => {
      e.preventDefault();
      if(e.keyCode === 13){
        self.Buttons.onCreateService();
      }
    }); 
  }

  modalClickEvents(){
    let modal = document.querySelector('.modal');
    let self = this;    
    modal.addEventListener('click', (e) => {
      if(e.target.classList.contains('modal')){
        modal.classList.toggle('shown');
      }

      if(e.target.id === 'yesBtn'){        
        let selectedService = modal.dataset.service;
        self.Buttons.onSave(e);
        switchService(selectedService);
        modal.classList.toggle('shown');
      }

      if(e.target.id === 'noBtn'){
        let selectedService = modal.dataset.service;
        switchService(selectedService);
        modal.classList.toggle('shown');
      }
    });
  }

  tooltipEvents(){
    let tooltip = document.querySelector('.tooltip .close');
    tooltip.addEventListener('click', (e) => {
      document.querySelector('.tooltip').classList.remove('shown');
    });
  }

  copyServiceClickEvent(){
    let copyServiceBtn = document.querySelector('#copy-to-clipboard');
    let copyToolTip = document.querySelector('.tooltip-clip');
    
    copyServiceBtn.addEventListener('click', (e) => {
      let serviceText = document.querySelector('#calls');
      serviceText.select();
    
      try {
        let successful = document.execCommand('copy');
        let msg = successful ? 'copied' : 'not copied';
        copyToolTip.textContent = msg;
        copyToolTip.style.display = 'block';
      } catch (err) {
        console.log('Oops, unable to copy');
      }
    });

    copyServiceBtn.addEventListener('mouseout', (e) => {
      copyToolTip.style.display = 'none';
    });
  }

  serviceSelectEvent(){
    let services = document.querySelector('#services');
    let self = this;    
    services.addEventListener('click', (e) => {
      if(e.target.tagName === 'LI'){
        onServiceSelect(e);
      }
    })

    function onServiceSelect(e) {
      if(self.Helpers.getSelectedService() && !document.querySelector('#saveBtn').disabled){
        document.querySelector('.unsaved').textContent = self.Helpers.getSelectedService();
        let modal = document.querySelector('.modal');
        modal.setAttribute('data-service', e.target.textContent);
        modal.classList.toggle('shown');
      } else {
        switchService(e.target.textContent);
      }  
    }    
  }
}