/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

import * as React from 'react'
const Marionette = require('marionette')
const template = require('./query-editor.hbs')
const CustomElements = require('../../js/CustomElements.js')
const QueryAdvanced = require('../query-advanced/query-advanced.view.js')
const QueryTitle = require('../query-title/query-title.view.js')
const store = require('../../js/store.js')
import ExtensionPoints from '../../extension-points'
import { showErrorMessages } from '../../react-component/utils/validation'
import ResultFormCollection from '../result-form/result-form-collection-instance'

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('query-editor'),
  regions: {
    queryContent: '> .editor-content > .content-form',
    queryTitle: '> .editor-content > .content-title',
  },
  originalType: '',
  events: {
    'click .editor-edit': 'edit',
    'click .editor-cancel': 'cancel',
    'click .editor-save': 'save',
    'click .editor-saveRun': 'saveRun',
  },
  initialize() {
    this.model = this.model._cloneOf
      ? store.getQueryById(this.model._cloneOf)
      : this.model
    this.listenTo(this.model, 'resetToDefaults change:type', this.reshow)
    this.listenTo(this.model, 'revert', this.revert)
    this.originalType = this.model.get('type')
  },
  revert() {
    if (this.model.get('type') !== this.originalType) {
      this.model.set('type', this.originalType)
    } else {
      this.reshow()
    }
  },
  reshow() {
    this.queryView = undefined
    const formType = this.model.get('type')
    switch (formType) {
      case 'custom':
        this.showCustom()
        break
      case 'text':
      case 'basic':
      case 'advanced':
        this.showForm(
          ExtensionPoints.queryForms.find(form => form.id === formType)
        )
        break
      default:
        const queryForm = ExtensionPoints.queryForms.find(
          form => form.id === formType
        )
        if (queryForm) {
          this.showQueryForm(queryForm)
        }
    }
    this.edit()
  },
  onBeforeShow() {
    this.reshow()
    this.showTitle()
  },
  showTitle() {
    this.queryTitle.show(
      new QueryTitle({
        model: this.model,
      })
    )
  },
  showForm(form) {
    const options = form.options || {}
    if (
      this.model.get('detail-level') === undefined ||
      this.resultFormIsInvalid(this.model.get('detail-level'))
    ) {
      this.model.set('detail-level', 'allFields')
    }
    this.queryContent.show(
      new form.view({
        model: this.model,
        ...options,
      })
    )
  },
  resultFormIsInvalid(resultFormTitle) {
    return (
      ResultFormCollection.getCollection().findWhere({
        title: resultFormTitle,
      }) === undefined
    )
  },
  showQueryForm(form) {
    const options = form.options || {}
    const queryFormView = Marionette.LayoutView.extend({
      template: () => (
        <form.view
          model={this.model}
          options={options}
          onRef={ref => (this.queryView = ref)}
        />
      ),
    })
    this.queryContent.show(new queryFormView({}))
  },
  showCustom() {
    this.queryContent.show(
      new QueryAdvanced({
        model: this.model,
        isForm: true,
        isFormBuilder: false,
      })
    )
  },
  handleEditOnShow() {
    if (this.$el.hasClass('is-editing')) {
      this.edit()
    }
  },
  showAdvanced() {
    this.queryContent.show(
      new QueryAdvanced({
        model: this.model,
      })
    )
  },
  edit() {
    this.$el.addClass('is-editing')
    if (!this.queryView) {
      this.queryContent.currentView.edit()
    }
  },
  cancel() {
    this.$el.removeClass('is-editing')
    this.onBeforeShow()
  },
  setDefaultTitle() {
    this.queryView
      ? this.queryView.setDefaultTitle()
      : this.queryContent.currentView.setDefaultTitle()
  },
  save() {
    const queryContentView = this.queryView
      ? this.queryView
      : this.queryContent.currentView
    const errorMessages = queryContentView.getErrorMessages()
    if (errorMessages.length !== 0) {
      showErrorMessages(errorMessages)
      return
    }
    queryContentView.save()
    this.queryTitle.currentView.save()
    if (this.model.get('title') === '') {
      this.setDefaultTitle()
    }
    if (store.getCurrentQueries().get(this.model) === undefined) {
      store.getCurrentQueries().add(this.model)
    }
    this.model.cqlSanitizePlugin()
    this.cancel()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    this.originalType = this.model.get('type')
  },
  saveRun() {
    const queryContentView = this.queryView
      ? this.queryView
      : this.queryContent.currentView
    const errorMessages = queryContentView.getErrorMessages()
    if (errorMessages.length !== 0) {
      showErrorMessages(errorMessages)
      return
    }
    queryContentView.save()
    this.queryTitle.currentView.save()
    if (this.model.get('title') === '') {
      this.setDefaultTitle()
    }
    if (store.getCurrentQueries().get(this.model) === undefined) {
      store.getCurrentQueries().add(this.model)
    }
    this.cancel()
    this.model.startSearchFromFirstPage()
    store.setCurrentQuery(this.model)
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    this.originalType = this.model.get('type')
  },
})
