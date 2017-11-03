package io.bluebank.hermes.core.socket

/**
 * Classes that implement this interface act both as a socket and a listener to another socket.
 * They are intermediaries acting as converters, or gates in the socket pipeline.
 */
interface SocketProcessor<R1, S1, R2, S2> : Socket<R1, S1>, SocketListener<R2, S2>

