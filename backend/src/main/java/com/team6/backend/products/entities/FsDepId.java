package com.team6.backend.products.entities;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Immutable;


@Entity
@Table(name="fs_dep_id")
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FsDepId {
    @Id
    @Column(name="dep_id")
    Short dep_id;

    @Column(name="dep_name")
    String dep_name;

    @OneToMany(mappedBy = "dep")
    private Set<Paknsave> depPaknsaves = new HashSet<>();

    @OneToMany(mappedBy = "dep")
    private Set<Newworld> depNewworlds = new HashSet<>();

}
