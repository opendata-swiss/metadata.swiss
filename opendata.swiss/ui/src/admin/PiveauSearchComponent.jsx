import React from "react";
import {AsyncTypeahead} from "react-bootstrap-typeahead";
import 'bootstrap/dist/css/bootstrap.min.css';
import 'react-bootstrap-typeahead/css/Typeahead.css';
import 'react-bootstrap-typeahead/css/Typeahead.bs5.css';
import {toArray} from "./util.js";

export default class PiveauSearchComponent extends React.Component {
  state = {
    isLoading: false,
  };

  prepareSearchUrl(searchParams = {}) {
    const url = new URL('search', this.props.field.get('piveau').get('search'))
    url.searchParams.set('filter', this.filter)
    for (const [key, value] of Object.entries(searchParams)) {
      url.searchParams.set(key, value)
    }
    return url
  }

  changed = (items) => {
    this.props.onChange(items)
  }
}

