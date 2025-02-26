/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * Renders all the components for the TaskReconcilePlan view.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.task.TaskReconcilePlan', {
  extend: 'Ext.container.Container',
  alias: 'widget.nx-coreui-task-reconcile-plan',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'checkbox',
        fieldLabel: NX.I18n.get('Task_TaskSettingsForm_PreviousPlan_FieldLabel'),
        helpText: NX.I18n.get('Task_TaskSettingsForm_PreviousPlan_Enable_HelpText'),
        name: 'runPreviousPlan',
        listeners: {
          change: function(_this) {
            _this.checked ? me.disableComponents(true) : me.disableComponents(false);
          }
        }
      },
      {
        xtype: 'label',
        html: NX.I18n.get('Task_TaskSettingsForm_PlanInformation_FieldLabel'),
        style: {
          display: "block",
          fontWeight: 'bold'
        }
      },
      {
        xtype: 'container',
        region: 'left',
        layout: {
          type: 'hbox',
          align: 'left',
          pack: 'left'
        },
        items: [
          {
            xtype: 'label',
            text: NX.I18n.get('Task_TaskSettingsForm_PlanInformation_PlanId_Info'),
            width: 100,
          },
          {
            xtype: 'label',
            itemId: 'labelPreviousPlanId',
            width: 'auto',
          }
        ]
      },
      {
        xtype: 'container',
        region: 'left',
        layout: {
          type: 'hbox',
          align: 'left',
          pack: 'left'
        },
        items: [
          {
            xtype: 'label',
            text: NX.I18n.get('Task_TaskSettingsForm_PlanInformation_Created_Info'),
            width: 100,
          },
          {
            xtype: 'label',
            itemId: 'labelPreviousPlanCreated',
            style: {
              width: 'auto',
              left: '100px'
            }
          }
        ]
      }
    ]

    me.callParent();
  },

  /**
   * Hides or shows the components based on the show value.
   * @param disabled
   */
  disableComponents: function(disabled) {
    var settingsForm = this.up('settingForm');
    if (settingsForm != null) {
      settingsForm.disableComponents(disabled);
    }
  },

  /**
   * Shows the previous plan if it exists.
   */
  showPreviousPlan: function() {
    var me = this;
    if ("blobstore.planReconciliation" === me.data.model.get('typeId')) {
      if (me.data.plan === null) {
        me.down('checkbox[name=runPreviousPlan]').setDisabled(true);
      }
    }
  },

  /**
   * Sets the plan information inside each HTML component.
   */
  setPlanInformation: function() {
    var me = this;

    var labelComponent = me.down('#labelPreviousPlanId');
    if (me.data.plan === null) {
      labelComponent.setText(NX.I18n.get('Task_TaskSettingsForm_PlanInformation_PlanId_NoPlans'));
      me.down('#labelPreviousPlanCreated').update("");
    }
    else {
      if (me.data.plansInPlannedState === 1) {
        console.log(me.data.plan.id);
        labelComponent.setText(me.data.plan.id);
      }
      else {
        labelComponent.setText(NX.I18n.get('Task_TaskSettingsForm_PlanInformation_PlanId_Multiple'));
      }
      console.log(me.data.plan.configuration['.created']);
      me.down('#labelPreviousPlanCreated').update(
          Ext.Date.format(new Date(me.data.plan.configuration['.created']),
              'Y-m-d\\TH:i:s.uP'));
    }
  },

  /**
   * Populates the data for the TaskReconcilePlan view.
   * @param model
   */
  populateData: function(model) {
    var timeLastPlan = 0;
    var plans = model.get('reconcilePlans').get('items');
    this.data = {
      plan: null,
      plansInPlannedState: 0,
      model: model,
      planIds: []
    };
    var data = this.data;
    // Iterate through the plans and find the most recent plan, only if it is in the PLANNED and EXECUTE state.
    plans.forEach(function(p) {
      if (p.state === 'PLANNED' || p.state === 'EXECUTE') {
        data.planIds.push(p.id);
        data.plansInPlannedState++;
        var date = new Date(p.configuration['.created']).getTime();
        if (timeLastPlan <= date) {
          timeLastPlan = date;
          data.plan = p;
        }
      }
    });
  },

  /**
   * Determines if the task is a plan reconciliation task.
   * @param model
   * @returns {boolean}
   */
  isPlanReconciliationTask: function(model) {
    return model !== null && "blobstore.planReconciliation" === model.get('typeId');
  },

  /**
   * Starts the component and populates the
   * @param model
   */
  startComponent: function(model) {
    var me = this;
    if (this.isPlanReconciliationTask(model)) {
      me.setDisabled(false);
      me.setHidden(false);
      me.populateData(model);
      me.setPlanInformation();
      if (this.data !== null) {
        if (this.data.plan === null) {
          this.down('checkbox[name=runPreviousPlan]').setDisabled(true);
          this.disableComponents(false);
          return;
        }
        var model = this.data.model;
        if (model.get('properties').hasOwnProperty('runPreviousPlan') &&
            model.get('properties')['runPreviousPlan'] === 'true') {
          this.disableComponents(true);
          me.up('form').getForm().setValues(model.get('properties'));
          return;
        }
        this.disableComponents(false);
      }
    }
  },

});
