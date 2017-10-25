import Buttons from 'scripts/buttons';
import { saveContent, switchService } from 'scripts/serviceList';

export default class EventListeners {
  constructor(){
    this.Buttons = new Buttons();
    this.init();
  }

  init(){
    this.textBoxMapEnterToCreate(); 
    this.modalEvents();    
  }

  textBoxMapEnterToCreate(){
    let textBoxCreate = document.querySelector('#newService');
    let self = this;
    textBoxCreate.addEventListener('keyup', (e) => {
      e.preventDefault();
      if(e.keyCode === 13){
        self.Buttons.onCreateService();
      }
    }); 
  }

  modalEvents(){
    let modal = document.querySelector('.modal');    
    modal.addEventListener('click', (e) => {
      if(e.target.classList.contains('modal')){
        modal.classList.toggle('shown');
      }

      if(e.target.id === 'yesBtn'){        
        let selectedService = modal.dataset.service;
        saveContent(selectedService);
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
}