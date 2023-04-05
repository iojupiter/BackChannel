defmodule BackChannelTest do
  use ExUnit.Case
  doctest BackChannel

  test "greets the world" do
    assert BackChannel.hello() == :world
  end
end
