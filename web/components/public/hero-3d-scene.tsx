"use client";

import { Canvas, useFrame } from "@react-three/fiber";
import { Float, Environment, MeshTransmissionMaterial } from "@react-three/drei";
import { useMemo, useRef } from "react";
import * as THREE from "three";

const BRAND_BLUE = "#2563eb";
const BRAND_AMBER = "#f59e0b";

/** DutyPe's mark is a location pin, so the hero object is a pin: sphere + cone. */
function LocationPin() {
  const group = useRef<THREE.Group>(null);

  useFrame((state) => {
    if (!group.current) return;
    const t = state.clock.elapsedTime;
    group.current.rotation.y = t * 0.35;
    group.current.position.y = Math.sin(t * 0.8) * 0.12;
  });

  return (
    <group ref={group} scale={1.15}>
      <mesh position={[0, 0.55, 0]}>
        <sphereGeometry args={[0.85, 48, 48]} />
        <MeshTransmissionMaterial
          thickness={0.9}
          roughness={0.08}
          transmission={1}
          ior={1.42}
          chromaticAberration={0.32}
          backside
          color={BRAND_BLUE}
        />
      </mesh>
      <mesh position={[0, -0.65, 0]} rotation={[Math.PI, 0, 0]}>
        <coneGeometry args={[0.62, 1.5, 48]} />
        <meshStandardMaterial
          color={BRAND_BLUE}
          metalness={0.85}
          roughness={0.18}
          emissive={BRAND_BLUE}
          emissiveIntensity={0.22}
        />
      </mesh>
      <mesh position={[0, 0.62, 0]}>
        <sphereGeometry args={[0.3, 32, 32]} />
        <meshStandardMaterial
          color="#ffffff"
          emissive={BRAND_AMBER}
          emissiveIntensity={0.55}
          roughness={0.25}
        />
      </mesh>
    </group>
  );
}

/** Orbiting cards standing in for job categories. */
function OrbitCards() {
  const group = useRef<THREE.Group>(null);
  const cards = useMemo(
    () =>
      Array.from({ length: 7 }, (_, i) => {
        const angle = (i / 7) * Math.PI * 2;
        return {
          angle,
          radius: 3.1 + (i % 3) * 0.32,
          height: Math.sin(angle * 1.6) * 0.85,
          hue: i % 2 === 0 ? BRAND_BLUE : BRAND_AMBER,
        };
      }),
    []
  );

  useFrame((state) => {
    if (group.current) group.current.rotation.y = state.clock.elapsedTime * 0.12;
  });

  return (
    <group ref={group}>
      {cards.map((card, i) => (
        <Float key={i} speed={1.4} rotationIntensity={0.5} floatIntensity={0.9}>
          <mesh
            position={[
              Math.cos(card.angle) * card.radius,
              card.height,
              Math.sin(card.angle) * card.radius,
            ]}
            rotation={[0, -card.angle, 0]}
          >
            <boxGeometry args={[0.95, 1.25, 0.04]} />
            <meshStandardMaterial
              color={card.hue}
              metalness={0.6}
              roughness={0.25}
              emissive={card.hue}
              emissiveIntensity={0.18}
            />
          </mesh>
        </Float>
      ))}
    </group>
  );
}

function Particles({ count = 220 }: { count?: number }) {
  const points = useRef<THREE.Points>(null);

  const positions = useMemo(() => {
    const array = new Float32Array(count * 3);
    for (let i = 0; i < count; i += 1) {
      array[i * 3] = (Math.random() - 0.5) * 16;
      array[i * 3 + 1] = (Math.random() - 0.5) * 9;
      array[i * 3 + 2] = (Math.random() - 0.5) * 10;
    }
    return array;
  }, [count]);

  useFrame((state) => {
    if (points.current) {
      points.current.rotation.y = state.clock.elapsedTime * 0.03;
    }
  });

  return (
    <points ref={points}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          count={count}
          array={positions}
          itemSize={3}
        />
      </bufferGeometry>
      <pointsMaterial size={0.045} color="#93c5fd" transparent opacity={0.7} />
    </points>
  );
}

/** Nudges the camera toward the pointer without re-rendering React. */
function PointerCamera() {
  useFrame((state) => {
    const { camera, pointer } = state;
    camera.position.x += (pointer.x * 1.4 - camera.position.x) * 0.04;
    camera.position.y += (pointer.y * 0.8 + 0.4 - camera.position.y) * 0.04;
    camera.lookAt(0, 0, 0);
  });
  return null;
}

export default function Hero3DScene() {
  return (
    <Canvas
      camera={{ position: [0, 0.4, 7.5], fov: 45 }}
      dpr={[1, 1.75]}
      gl={{ antialias: true, alpha: true, powerPreference: "high-performance" }}
    >
      <ambientLight intensity={0.45} />
      <directionalLight position={[4, 6, 5]} intensity={1.5} />
      <pointLight position={[-5, -2, -4]} intensity={2.2} color={BRAND_BLUE} />
      <pointLight position={[4, 3, 2]} intensity={1.1} color={BRAND_AMBER} />

      <LocationPin />
      <OrbitCards />
      <Particles />
      <PointerCamera />

      <Environment preset="city" />
    </Canvas>
  );
}
