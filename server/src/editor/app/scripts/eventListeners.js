import Buttons from 'scripts/buttons';
import { saveContent, switchService } from 'scripts/serviceList';
import Helpers from 'scripts/helpers';

export default class EventListeners {
  constructor(){
    this.Buttons = new Buttons();
    this.Helpers = new Helpers();
    this.init();
  }

  init(){
    this.textBoxKeyEvents(); 
    this.modalClickEvents();    
    this.popupEvents();
    this.copyService();
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

    //textBoxCreate.addEventListener('input', (e) => {      
    //  textBoxCreate.value = self.Helpers.checkCreateService(e.target.value);
    //});
  }

  modalClickEvents(){
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

  popupEvents(){
    let popup = document.querySelector('.tooltip .close');
    popup.addEventListener('click', (e) => {
      document.querySelector('.tooltip').classList.remove('shown');
    });
  }

  copyService(){
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