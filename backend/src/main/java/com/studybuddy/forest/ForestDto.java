package com.studybuddy.forest;

import java.util.List;

/** A student's planted trees, newest first, with totals and the growth stages for the legend. */
public record ForestDto(List<TreeDto> trees, int treeCount, long focusedMinutes, long longestMinutes,
    List<TreeStageDto> stages, int variantsPerSpecies) {}
