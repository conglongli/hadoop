/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.protocol.datatransfer;

import static org.apache.hadoop.hdfs.protocol.datatransfer.DataTransferProtoUtil.toProto;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.CachingStrategyProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.ChecksumProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.ClientOperationHeaderProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.DataTransferTraceInfoProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.OpBlockChecksumProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.OpCopyBlockProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.OpReadBlockProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.OpReplaceBlockProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.OpRequestShortCircuitAccessProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.OpTransferBlockProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.OpWriteBlockProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.ReleaseShortCircuitAccessRequestProto;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.ShortCircuitShmRequestProto;
import org.apache.hadoop.hdfs.protocolPB.PBHelperClient;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
import org.apache.hadoop.hdfs.server.datanode.CachingStrategy;
import org.apache.hadoop.hdfs.shortcircuit.ShortCircuitShm.SlotId;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.util.DataChecksum;

import org.apache.htrace.core.SpanId;
import org.apache.htrace.core.Tracer;

import com.google.protobuf.Message;

/** Sender */
@InterfaceAudience.Private
@InterfaceStability.Evolving
public class Sender implements DataTransferProtocol {
  private final DataOutputStream out;
  // Conglong
  private InetAddress cl_ip;
  private String cl_hostname = "";

  /** Create a sender for DataTransferProtocol with a output stream. */
  public Sender(final DataOutputStream out) {
    this.out = out;
    try {
      this.cl_ip = InetAddress.getLocalHost();
      this.cl_hostname = (this.cl_ip).getHostName(); 
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }

  /** Initialize a operation. */
  private static void op(final DataOutput out, final Op op) throws IOException {
    out.writeShort(DataTransferProtocol.DATA_TRANSFER_VERSION);
    op.write(out);
  }

  private static void send(final DataOutputStream out, final Op opcode,
      final Message proto) throws IOException {
    LOG.trace("Sending DataTransferOp {}: {}",
        proto.getClass().getSimpleName(), proto);
    op(out, opcode);
    proto.writeDelimitedTo(out);
    out.flush();
  }

  private static void send_readblock(final DataOutputStream out, final Op opcode,
      final Message proto, final ExtendedBlock blk, final DatanodeID datanodeID) throws IOException {
    String cll_hostname = "";
    try {
      cll_hostname = (InetAddress.getLocalHost()).getHostName(); 
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    LOG.info("Conglong Read Est 84 Sender Starting read blockId {} length {} from {} to {}",
        blk.getBlockId(), blk.getNumBytes(), datanodeID.getHostName(), cll_hostname);
    //LOG.trace("Sending DataTransferOp {}: {}",
    //    proto.getClass().getSimpleName(), proto);
    LOG.info("Conglong Read Est 88 Sender Starting read blockId {} length {} from {} to {}",
        blk.getBlockId(), blk.getNumBytes(), datanodeID.getHostName(), cll_hostname);
    op(out, opcode);
    LOG.info("Conglong Read Est 91 Sender Starting read blockId {} length {} from {} to {}",
        blk.getBlockId(), blk.getNumBytes(), datanodeID.getHostName(), cll_hostname);
    proto.writeDelimitedTo(out);
    LOG.info("Conglong Read Est 94 Sender Starting read blockId {} length {} from {} to {}",
        blk.getBlockId(), blk.getNumBytes(), datanodeID.getHostName(), cll_hostname);
    out.flush();
    LOG.info("Conglong Read Est 97 Sender Starting read blockId {} length {} from {} to {}",
        blk.getBlockId(), blk.getNumBytes(), datanodeID.getHostName(), cll_hostname);
  }

  private static void send_writeblock(final DataOutputStream out, final Op opcode,
      final Message proto, final ExtendedBlock blk) throws IOException {
    //LOG.info("Conglong Write Est 103 Sender Starting read blockId {} length {}",
    //    blk.getBlockId(), blk.getNumBytes());
    //LOG.trace("Sending DataTransferOp {}: {}",
    //    proto.getClass().getSimpleName(), proto);
    //LOG.info("Conglong Write Est 107 Sender Starting read blockId {} length {}",
    //    blk.getBlockId(), blk.getNumBytes());
    op(out, opcode);
    //LOG.info("Conglong Write Est 110 Sender Starting read blockId {} length {}",
    //    blk.getBlockId(), blk.getNumBytes());
    proto.writeDelimitedTo(out);
    //LOG.info("Conglong Write Est 113 Sender Starting read blockId {} length {}",
    //    blk.getBlockId(), blk.getNumBytes());
    out.flush();
    //LOG.info("Conglong Write Est 116 Sender Starting read blockId {} length {}",
    //    blk.getBlockId(), blk.getNumBytes());
  }

  static private CachingStrategyProto getCachingStrategy(
      CachingStrategy cachingStrategy) {
    CachingStrategyProto.Builder builder = CachingStrategyProto.newBuilder();
    if (cachingStrategy.getReadahead() != null) {
      builder.setReadahead(cachingStrategy.getReadahead());
    }
    if (cachingStrategy.getDropBehind() != null) {
      builder.setDropBehind(cachingStrategy.getDropBehind());
    }
    return builder.build();
  }

  @Override
  public void readBlock(final ExtendedBlock blk,
      final Token<BlockTokenIdentifier> blockToken,
      final String clientName,
      final long blockOffset,
      final long length,
      final boolean sendChecksum,
      final CachingStrategy cachingStrategy) throws IOException {
    LOG.info("Conglong Read Est 140 Sender Starting read blockId {} length {} from {} to {}",
        blk.getBlockId(), blk.getNumBytes(), "", cl_hostname);
    OpReadBlockProto proto = OpReadBlockProto.newBuilder()
        .setHeader(DataTransferProtoUtil.buildClientHeader(blk, clientName,
            blockToken))
        .setOffset(blockOffset)
        .setLen(length)
        .setSendChecksums(sendChecksum)
        .setCachingStrategy(getCachingStrategy(cachingStrategy))
        .build();
    LOG.info("Conglong Read Est 150 Sender Starting read blockId {} length {} from {} to {}",
        blk.getBlockId(), blk.getNumBytes(), "", cl_hostname);
    send(out, Op.READ_BLOCK, proto);
    //send_readblock(out, Op.READ_BLOCK, proto, blk);
    //LOG.info("Conglong Read Est 154 Sender");
  }

  public void readBlock2(final ExtendedBlock blk,
      final Token<BlockTokenIdentifier> blockToken,
      final String clientName,
      final long blockOffset,
      final long length,
      final boolean sendChecksum,
      final CachingStrategy cachingStrategy,
      final DatanodeID datanodeID) throws IOException {
    LOG.info("Conglong Read Est 140 Sender Starting read blockId {} length {} from {} to {}",
        blk.getBlockId(), blk.getNumBytes(), datanodeID.getHostName(), cl_hostname);
    OpReadBlockProto proto = OpReadBlockProto.newBuilder()
        .setHeader(DataTransferProtoUtil.buildClientHeader(blk, clientName,
            blockToken))
        .setOffset(blockOffset)
        .setLen(length)
        .setSendChecksums(sendChecksum)
        .setCachingStrategy(getCachingStrategy(cachingStrategy))
        .build();
    LOG.info("Conglong Read Est 150 Sender Starting read blockId {} length {} from {} to {}",
        blk.getBlockId(), blk.getNumBytes(), datanodeID.getHostName(), cl_hostname);
    //send(out, Op.READ_BLOCK, proto);
    send_readblock(out, Op.READ_BLOCK, proto, blk, datanodeID);
    //LOG.info("Conglong Read Est 154 Sender");
  }


  @Override
  public void writeBlock(final ExtendedBlock blk,
      final StorageType storageType,
      final Token<BlockTokenIdentifier> blockToken,
      final String clientName,
      final DatanodeInfo[] targets,
      final StorageType[] targetStorageTypes,
      final DatanodeInfo source,
      final BlockConstructionStage stage,
      final int pipelineSize,
      final long minBytesRcvd,
      final long maxBytesRcvd,
      final long latestGenerationStamp,
      DataChecksum requestedChecksum,
      final CachingStrategy cachingStrategy,
      final boolean allowLazyPersist,
      final boolean pinning,
      final boolean[] targetPinnings) throws IOException {
    ClientOperationHeaderProto header = DataTransferProtoUtil.buildClientHeader(
        blk, clientName, blockToken);

    ChecksumProto checksumProto =
        DataTransferProtoUtil.toProto(requestedChecksum);

    OpWriteBlockProto.Builder proto = OpWriteBlockProto.newBuilder()
        .setHeader(header)
        .setStorageType(PBHelperClient.convertStorageType(storageType))
        .addAllTargets(PBHelperClient.convert(targets, 1))
        .addAllTargetStorageTypes(
            PBHelperClient.convertStorageTypes(targetStorageTypes, 1))
        .setStage(toProto(stage))
        .setPipelineSize(pipelineSize)
        .setMinBytesRcvd(minBytesRcvd)
        .setMaxBytesRcvd(maxBytesRcvd)
        .setLatestGenerationStamp(latestGenerationStamp)
        .setRequestedChecksum(checksumProto)
        .setCachingStrategy(getCachingStrategy(cachingStrategy))
        .setAllowLazyPersist(allowLazyPersist)
        .setPinning(pinning)
        .addAllTargetPinnings(PBHelperClient.convert(targetPinnings, 1));

    if (source != null) {
      proto.setSource(PBHelperClient.convertDatanodeInfo(source));
    }

    //send(out, Op.WRITE_BLOCK, proto.build());
    send_writeblock(out, Op.WRITE_BLOCK, proto.build(), blk);
  }

  @Override
  public void transferBlock(final ExtendedBlock blk,
      final Token<BlockTokenIdentifier> blockToken,
      final String clientName,
      final DatanodeInfo[] targets,
      final StorageType[] targetStorageTypes) throws IOException {

    OpTransferBlockProto proto = OpTransferBlockProto.newBuilder()
        .setHeader(DataTransferProtoUtil.buildClientHeader(
            blk, clientName, blockToken))
        .addAllTargets(PBHelperClient.convert(targets))
        .addAllTargetStorageTypes(
            PBHelperClient.convertStorageTypes(targetStorageTypes))
        .build();

    send(out, Op.TRANSFER_BLOCK, proto);
  }

  @Override
  public void requestShortCircuitFds(final ExtendedBlock blk,
      final Token<BlockTokenIdentifier> blockToken,
      SlotId slotId, int maxVersion, boolean supportsReceiptVerification)
      throws IOException {
    OpRequestShortCircuitAccessProto.Builder builder =
        OpRequestShortCircuitAccessProto.newBuilder()
            .setHeader(DataTransferProtoUtil.buildBaseHeader(
                blk, blockToken)).setMaxVersion(maxVersion);
    if (slotId != null) {
      builder.setSlotId(PBHelperClient.convert(slotId));
    }
    builder.setSupportsReceiptVerification(supportsReceiptVerification);
    OpRequestShortCircuitAccessProto proto = builder.build();
    send(out, Op.REQUEST_SHORT_CIRCUIT_FDS, proto);
  }

  @Override
  public void releaseShortCircuitFds(SlotId slotId) throws IOException {
    ReleaseShortCircuitAccessRequestProto.Builder builder =
        ReleaseShortCircuitAccessRequestProto.newBuilder().
            setSlotId(PBHelperClient.convert(slotId));
    SpanId spanId = Tracer.getCurrentSpanId();
    if (spanId.isValid()) {
      builder.setTraceInfo(DataTransferTraceInfoProto.newBuilder().
          setTraceId(spanId.getHigh()).
          setParentId(spanId.getLow()));
    }
    ReleaseShortCircuitAccessRequestProto proto = builder.build();
    send(out, Op.RELEASE_SHORT_CIRCUIT_FDS, proto);
  }

  @Override
  public void requestShortCircuitShm(String clientName) throws IOException {
    ShortCircuitShmRequestProto.Builder builder =
        ShortCircuitShmRequestProto.newBuilder().
            setClientName(clientName);
    SpanId spanId = Tracer.getCurrentSpanId();
    if (spanId.isValid()) {
      builder.setTraceInfo(DataTransferTraceInfoProto.newBuilder().
          setTraceId(spanId.getHigh()).
          setParentId(spanId.getLow()));
    }
    ShortCircuitShmRequestProto proto = builder.build();
    send(out, Op.REQUEST_SHORT_CIRCUIT_SHM, proto);
  }

  @Override
  public void replaceBlock(final ExtendedBlock blk,
      final StorageType storageType,
      final Token<BlockTokenIdentifier> blockToken,
      final String delHint,
      final DatanodeInfo source) throws IOException {
    OpReplaceBlockProto proto = OpReplaceBlockProto.newBuilder()
        .setHeader(DataTransferProtoUtil.buildBaseHeader(blk, blockToken))
        .setStorageType(PBHelperClient.convertStorageType(storageType))
        .setDelHint(delHint)
        .setSource(PBHelperClient.convertDatanodeInfo(source))
        .build();

    send(out, Op.REPLACE_BLOCK, proto);
  }

  @Override
  public void copyBlock(final ExtendedBlock blk,
      final Token<BlockTokenIdentifier> blockToken) throws IOException {
    OpCopyBlockProto proto = OpCopyBlockProto.newBuilder()
        .setHeader(DataTransferProtoUtil.buildBaseHeader(blk, blockToken))
        .build();

    send(out, Op.COPY_BLOCK, proto);
  }

  @Override
  public void blockChecksum(final ExtendedBlock blk,
      final Token<BlockTokenIdentifier> blockToken) throws IOException {
    OpBlockChecksumProto proto = OpBlockChecksumProto.newBuilder()
        .setHeader(DataTransferProtoUtil.buildBaseHeader(blk, blockToken))
        .build();

    send(out, Op.BLOCK_CHECKSUM, proto);
  }
}
