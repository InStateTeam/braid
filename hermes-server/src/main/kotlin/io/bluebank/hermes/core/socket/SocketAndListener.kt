package io.bluebank.hermes.core.socket

interface SocketAndListener<R1, S1, R2, S2> : Socket<R1, S1>, SocketListener<R2, S2>

