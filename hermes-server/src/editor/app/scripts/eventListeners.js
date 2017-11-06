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
    this.popupEvents();
    this.copyServiceClickEvent();
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

  popupEvents(){
    let popup = document.querySelector('.tooltip .close');
    popup.addEventListener('click', (e) => {
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
}