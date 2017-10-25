export default class Helpers {
  
  getSelectedService() {
    return selectedService;
  }    

  setSelectedService(service) {
    selectedService = service;
  }

  setEditorContents(script) {
    editor.updateOptions({readOnly: false});
    editor.setValue(script)
  }

  getServiceScript(serviceName, callback) {
    $.get("/api/services/" + serviceName + "/script", function(script) {
      callback(script)
    })
  }

  getJavaHeaders(serviceName, callback) {
    $.get("/api/services/" + serviceName + "/java", function(script) {
      callback(script);
    });
  }
  
  parseURL(url) {
    var parser = document.createElement('a'),
      searchObject = {},
      queries, split, i;
    // Let the browser do the work
    parser.href = url;
    // Convert query string to object
    queries = parser.search.replace(/^\?/, '').split('&');
    for( i = 0; i < queries.length; i++ ) {
      split = queries[i].split('=');
      searchObject[split[0]] = split[1];
    }
    return {
      protocol: parser.protocol,
      host: parser.host,
      hostname: parser.hostname,
      port: parser.port,
      pathname: parser.pathname,
      search: parser.search,
      searchObject: searchObject,
      hash: parser.hash
    };
  }

  parseCreateService(string){
    // TODO: delete offending char
    const pattern = /^[A-Za-z]\w*$/;
    if(string.length > 0){
      let result = pattern.test(string);
      if(!result){
        return string.slice(0, (string.length - 1));
      }
    }
    return string
  }
  
  selectHighlight(selectedService){
    const lists = document.querySelector('#services').querySelectorAll('li');
    let selectedLi;
    for (let item = 0; item < lists.length; item++) {
      lists[item].style.background = '#000';
      if(lists[item].textContent == selectedService){
        selectedLi = lists[item];
      }
    }
    selectedLi.style.background = "#EF0017"
  }

  highlightNewService(selectedService){
    const lists = document.querySelector('#services').querySelectorAll('li');
    let newServiceIndex = "";
    for (let item = 0; item < lists.length;  item++) {
      lists[item].style.background = '#000';
      if(lists[item].innerText === selectedService){
        newServiceIndex = item;
      }
    }
    if(newServiceIndex){
      lists[newServiceIndex].style.background = "#EF0017";
    }
  }

  populateList(list, serviceArray){
    while(list.firstChild){
      list.removeChild(list.firstChild);
    }

    serviceArray.map((service) => {
      let text, textNode;
      let node = document.createElement("LI");

      let title = document.createElement("H3");
      title.classList.add('title');
      text = service.name;
      textNode = document.createTextNode(text);
      title.appendChild(textNode);
      node.appendChild(title);

      let content = document.createElement("DIV");
      content.classList.add('content', 'hide');
      node.appendChild(content);

      let description = document.createElement("DIV");
      description.classList.add('description');
      content.appendChild(description);

      let descriptionTitle = document.createElement('H4');
      descriptionTitle.classList.add('description-title');
      text = 'Description';
      textNode = document.createTextNode(text);
      descriptionTitle.appendChild(textNode);
      description.appendChild(descriptionTitle);

      let descriptionContent = document.createElement('P');
      descriptionContent.classList.add('description-content');
      text = service.description;
      textNode = document.createTextNode(text);
      descriptionContent.appendChild(textNode);
      description.appendChild(descriptionContent);

      let parameters = document.createElement("DIV");
      content.appendChild(parameters);

      let parametersTitle = document.createElement("H4");
      parametersTitle.classList.add('parameters-title');
      text = 'Parameters';
      textNode = document.createTextNode(text);
      parametersTitle.appendChild(textNode);
      parameters.appendChild(parametersTitle);

      let parametersContent = document.createElement('P');
      parametersContent.classList.add('parameters-content');
      text = service.parameters;
      textNode = document.createTextNode(text);
      parametersContent.appendChild(textNode);
      parameters.appendChild(parametersContent);

      let returnType = document.createElement("DIV");
      returnType.classList.add('return-type');
      content.appendChild(returnType);

      let returnTypeTitle = document.createElement("H4");
      returnTypeTitle.classList.add("return-type-title");
      textNode = document.createTextNode("Return type");
      returnTypeTitle.appendChild(textNode);
      returnType.appendChild(returnTypeTitle);

      let returnTypeContent = document.createElement("P");
      returnTypeContent.classList.add('return-type-content');
      text = service.returnType;
      textNode = document.createTextNode(text);
      returnTypeContent.appendChild(textNode);
      returnType.appendChild(returnTypeContent);


      list.appendChild(node);

      node.addEventListener('click', (e) => {
        content.classList.toggle('hide');
      });
    });
  }

  populateFunctions(serviceArray){
    const functionList = document.querySelector('.implemented-functions');
    this.populateList(functionList, serviceArray);
  }

  populateStubs(serviceArray){
    const stubList = document.querySelector('.stubbed-functions');
    this.populateList(stubList, serviceArray);
  }

  expandFunctionsSection(){
    const funSection = document.querySelector('.calls');
    const editor = document.querySelector('#editor');
    funSection.style.marginLeft = '250px'
    editor.style.width =  'calc(100% - 480px)'
    editor.style.marginLeft = '230px'
    setTimeout(() => {funSection.style.zIndex = 1;}, 500);
  }

  collapseFunctionsSection(){
    const funSection = document.querySelector('.calls');
    const editor = document.querySelector('#editor');
    funSection.style.zIndex = -1;
    funSection.style.marginLeft = '0px'
    editor.style.width =  'calc(100% - 250px)'
    editor.style.marginLeft = '0px'
  }
}

  