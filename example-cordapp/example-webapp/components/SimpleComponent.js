/**
 * A simple class for setting up custom html elements
 */
class SimpleComponent extends HTMLElement {
  constructor(templateId) {
    super();
    const importDoc = document.currentScript.ownerDocument; // importee
    const template = importDoc.querySelector('#' + templateId);
    this.attachShadow({mode: 'open'});
    this.shadowRoot.appendChild(template.content.cloneNode(true));
  }

  $(selector) {
    return this.shadowRoot.querySelector(selector);
  }
}
