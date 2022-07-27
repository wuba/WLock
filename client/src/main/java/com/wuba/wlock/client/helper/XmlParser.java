/*
 * Copyright (C) 2005-present, 58.com.  All rights reserved.
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
package com.wuba.wlock.client.helper;

import com.wuba.wlock.client.registryclient.entity.ClientKeyEntity;
import com.wuba.wlock.client.registryclient.entity.GroupNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class XmlParser {
	
	private Document document;
	private static final Log logger = LogFactory.getLog(XmlParser.class);

	private String convertXml2String() throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		t.transform(new DOMSource(document), new StreamResult(bos));
		return bos.toString();
	}

	private void init() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			this.document = builder.newDocument();
		} catch (ParserConfigurationException e) {
			logger.error(e);
		}
	}
	
	public String createClusterXml(ClientKeyEntity clientKey) throws TransformerException {
		init();
		Element root = document.createElement("clusters");
		document.appendChild(root);
		Element child = document.createElement("cluster");
		root.appendChild(child);
		child.setAttribute("id", String.valueOf(clientKey.getHashCode()));
		child.setAttribute("version", String.valueOf(clientKey.getVersion()));
		child.setAttribute("key", String.valueOf(clientKey.getKey()));
		child.setAttribute("autoRenew", String.valueOf(clientKey.getAutoRenew()));

		//生成multiGroup标签
		Element multiGroup = document.createElement("multiGroup");
		Element allNode = document.createElement("allNode");
		Map<Integer, com.wuba.wlock.client.registryclient.entity.Node> allNodeMap = clientKey.getAllNodeMap();
		if (allNodeMap != null && !allNodeMap.isEmpty()) {
			StringBuilder allNodeSb = new StringBuilder();
			for (Map.Entry<Integer, com.wuba.wlock.client.registryclient.entity.Node> entry: allNodeMap.entrySet()) {
				com.wuba.wlock.client.registryclient.entity.Node node = entry.getValue();
				allNodeSb.append(String.format("%s:%d:%d,", node.getIp(), node.getPort(), node.getSequence()));
			}
			allNode.setTextContent(allNodeSb.toString());
		}

		multiGroup.appendChild(allNode);

		List<GroupNode> groupNodeList = clientKey.getGroupNodeList();
		if (groupNodeList != null && !groupNodeList.isEmpty()) {
			for (GroupNode groupNode: groupNodeList) {
				Element groupNodeElement = document.createElement("groupNode");
				groupNodeElement.setAttribute("groupId", String.valueOf(groupNode.getGroupId()));
				groupNodeElement.setAttribute("masterNode", String.valueOf(groupNode.getMasterNode()));
				groupNodeElement.setAttribute("masterVersion", String.valueOf(groupNode.getMasterVersion()));
				StringBuilder nodeListSb = new StringBuilder();
				List<Integer> nodeList = groupNode.getNodeList();
				if (nodeList != null && !nodeList.isEmpty()) {
					for (Integer nodeId: nodeList) {
						nodeListSb.append(nodeId).append(",");
					}
				}
				groupNodeElement.setAttribute("nodeList", nodeListSb.toString());
				multiGroup.appendChild(groupNodeElement);
			}
			root.appendChild(multiGroup);
		}

		return convertXml2String();
	}

	public static ClientKeyEntity parseClusterXmlFromFile(String filePath) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
		ClientKeyEntity clientKey = new ClientKeyEntity();
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		XPathExpression exprProperty = xpath.compile("/clusters/cluster");

		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		Document doc = builder.parse(filePath);
		NodeList propertyNodes = (NodeList) exprProperty.evaluate(doc, XPathConstants.NODESET);
		for (int i = 0; i < propertyNodes.getLength(); i++) {
			Node node = propertyNodes.item(i);
			clientKey.setHashCode(Integer.parseInt(node.getAttributes().getNamedItem("id").getNodeValue()));
			clientKey.setVersion(Long.parseLong(node.getAttributes().getNamedItem("version").getNodeValue()));
			clientKey.setKey(node.getAttributes().getNamedItem("key").getNodeValue());
			clientKey.setAutoRenew(Boolean.parseBoolean(node.getAttributes().getNamedItem("autoRenew").getNodeValue()));
		}

		XPathExpression multiGroupXPathExpression = xpath.compile("/clusters/multiGroup");
		NodeList multiGroupNodeList = (NodeList) multiGroupXPathExpression.evaluate(doc, XPathConstants.NODESET);
		if (multiGroupNodeList != null && multiGroupNodeList.getLength() > 0) {
			Element multiGroupElement = (Element) multiGroupNodeList.item(0);
			NodeList allNodeNodeList = multiGroupElement.getElementsByTagName("allNode");
			if (allNodeNodeList != null && allNodeNodeList.getLength() > 0) {
				String allNodeText = allNodeNodeList.item(0).getTextContent();
				if (allNodeText != null && !allNodeText.isEmpty()) {

					String[] allNodeSplit = allNodeText.split(",");
					if (allNodeSplit.length > 0) {
						Map<Integer, com.wuba.wlock.client.registryclient.entity.Node> allNodeMap = new HashMap<Integer, com.wuba.wlock.client.registryclient.entity.Node>();

						for (String nodeText: allNodeSplit) {
							String[] split = nodeText.split(":");
							String ip = split[0];
							int port = Integer.parseInt(split[1]);
							int sequence = Integer.parseInt(split[2]);
							allNodeMap.put(sequence, new com.wuba.wlock.client.registryclient.entity.Node(ip, port, sequence));
						}

						clientKey.setAllNodeMap(allNodeMap);
					}
				}
			}

			NodeList groupNodeNodeList = multiGroupElement.getElementsByTagName("groupNode");
			if (groupNodeNodeList != null && groupNodeNodeList.getLength() > 0) {
				List<GroupNode> groupNodeList = new ArrayList<GroupNode>();
				for (int i = 0; i < groupNodeNodeList.getLength(); i++) {
					Node groupNodeNode = groupNodeNodeList.item(i);
					NamedNodeMap attributes = groupNodeNode.getAttributes();
					GroupNode groupNode = new GroupNode();
					groupNode.setGroupId(Integer.parseInt(attributes.getNamedItem("groupId").getNodeValue()));
					groupNode.setMasterNode(Integer.parseInt(attributes.getNamedItem("masterNode").getNodeValue()));
					groupNode.setMasterVersion(Long.parseLong(attributes.getNamedItem("masterVersion").getNodeValue()));
					List<Integer> nodeIdList = new ArrayList<Integer>();
					String nodeListText = attributes.getNamedItem("nodeList").getNodeValue();
					String[] split = nodeListText.split(",");
					for (String nodeIdValue: split) {
						nodeIdList.add(Integer.parseInt(nodeIdValue));
					}
					groupNode.setNodeList(nodeIdList);

					groupNodeList.add(groupNode);
				}

				clientKey.setGroupNodeList(groupNodeList);
			}
		}
		return clientKey;
	}

}
