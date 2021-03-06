package org.bridgedb.cytoscape.internal.task;

/*******************************************************************************
 * Copyright 2010-2013 BridgeDb App developing team
 * 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.bridgedb.cytoscape.internal.AttributeBasedIDMappingImpl;
import org.bridgedb.cytoscape.internal.util.DataSourceWrapper;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import org.cytoscape.model.CyNetwork;

import java.util.Set;
import java.util.Map;
import org.bridgedb.cytoscape.internal.AttributeBasedIDMapping;
import org.bridgedb.cytoscape.internal.IDMapperClientManager;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.Tunable;
/**
 *
 */
public class AttributeBasedIDMappingTask extends AbstractTask implements ObservableTask {
    @Tunable(description="Network to mapping identifiers in",context="nogui")
    public CyNetwork network;
    
    @Tunable(description="Source column in node table",context="nogui")
    public String sourceColumn;
    
    @Tunable(description="Source ID type",context="nogui")
    public String sourceIdType;
    
    @Tunable(description="Target column in node table",context="nogui")
    public String targetColumn;
    
    @Tunable(description="Target ID type",context="nogui")
    public String targetIdType;
    
    @Tunable(description="Application name (optional) for application-specific ID mapping resources"
            + " -- do not specify if use the globel resources", context="nogui")
    public String appName = null;
    
    private Map<String,Set<DataSourceWrapper>> mapSrcAttrIDTypes;
    private Map<String, DataSourceWrapper> mapTgtAttrNameIDType;
    private Map<String,Class<?>> mapTgtAttrNameAttrType;
    
    AttributeBasedIDMapping mappingService;
    
    private boolean success = false;
    
    private boolean byCommand = true;
    
    public AttributeBasedIDMappingTask() {
    }

	/**
         * 
         * @param networks
         * @param mapSrcAttrIDTypes
         * @param mapTgtAttrNameIDType
         */
	public AttributeBasedIDMappingTask(final CyNetwork network,
                                       final Map<String,Set<DataSourceWrapper>> mapSrcAttrIDTypes,
                                       final Map<String, DataSourceWrapper> mapTgtAttrNameIDType,
                                       Map<String,Class<?>> mapTgtAttrNameAttrType) {
            byCommand = false;
            this.network = network;
            this.mapSrcAttrIDTypes = mapSrcAttrIDTypes;
            this.mapTgtAttrNameIDType = mapTgtAttrNameIDType;
            this.mapTgtAttrNameAttrType = mapTgtAttrNameAttrType;
	}
        
    @Override
        public void cancel() {
            mappingService.interrupt();
            success = false;
        }

	/**
	 * Executes Task.
	 */
    //@Override
	public void run(final TaskMonitor taskMonitor) {
            mappingService = new AttributeBasedIDMappingImpl(taskMonitor, IDMapperClientManager.getIDMapperClientManager(appName));
            if (byCommand && !convertCommandParameters(taskMonitor)) {
                return;
            }
            
		 taskMonitor.setTitle("Mapping identifiers");
		 try {
			 mappingService.map(network, mapSrcAttrIDTypes, mapTgtAttrNameIDType, mapTgtAttrNameAttrType, byCommand?-1:100);
                         success = true;
		 } catch (Exception e) {
			 taskMonitor.showMessage(TaskMonitor.Level.ERROR,"ID mapping failed.\n");
			 e.printStackTrace();
		 }
	}
        
        public boolean success() {
            return success;
        }

        public String getResults(Class type)  {
            return mappingService.getReport();
        }
        
    private boolean convertCommandParameters(final TaskMonitor taskMonitor) {
        if (network == null) {
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Please specify a network.");
            return false;
        }
        
        if (sourceColumn == null) {
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Please specify source attribute.");
            return false;
        }
        
        if (null == network.getDefaultNodeTable().getColumn(sourceColumn)) {
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Could not find source node attribute "
                    +sourceColumn);
            return false;
        }
        
        if (sourceIdType == null) {
            taskMonitor.setStatusMessage("Please specify source ID type.");
            return false;
        }
        
        IDMapperClientManager idMapperClientManager = IDMapperClientManager.getIDMapperClientManager(appName);
        Set<DataSourceWrapper> srcDataSources = idMapperClientManager.getSupportedSrcTypes();
        Set<DataSourceWrapper> tgtDataSources = idMapperClientManager.getSupportedTgtTypes();
        if (srcDataSources==null || srcDataSources.isEmpty()) {
            taskMonitor.setStatusMessage("No supported source or target id type."
                    + " Please select mapping resources first.");
            return false;
        }
        
        DataSourceWrapper srcDsw = DataSourceWrapper.getInstance(sourceIdType);
        if (!srcDataSources.contains(srcDsw)) {
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Could not find source ID type "
                    +sourceIdType);
            return false;
        }
        
        DataSourceWrapper tgtDsw = DataSourceWrapper.getInstance(targetIdType);
        if (!tgtDataSources.contains(tgtDsw)) {
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Could not find target ID type "
                    +targetIdType);
            return false;
        }
        
        if (targetColumn == null) {
            taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Please specify target attribute.");
            return false;
        }
        
        if (null == network.getDefaultNodeTable().getColumn(targetColumn)) {
            taskMonitor.showMessage(TaskMonitor.Level.INFO, "Could not find target node attribute "
                    +targetColumn+". A new attribute in node table will be created.");
        }
        
        mapSrcAttrIDTypes = Collections.singletonMap(sourceColumn, Collections.singleton(srcDsw));
        mapTgtAttrNameIDType = Collections.singletonMap(targetColumn, tgtDsw);
        mapTgtAttrNameAttrType = new HashMap<String,Class<?>>(1);
        mapTgtAttrNameAttrType.put(targetColumn, List.class); // why could not use singleton?
        
        return true;
    }
}
